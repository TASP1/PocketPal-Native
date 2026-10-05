package com.tasp1.pocketpal.network

import com.tasp1.pocketpal.domain.HealthStatus
import com.tasp1.pocketpal.domain.RemoteModel
import com.tasp1.pocketpal.protocol.StreamEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible Kaggle Bridge client — health, models, chat, SSE stream.
 * API key is the BRIDGE_KEY (not Kaggle KGAT).
 */
class BridgeClient(
    private var baseUrl: String,
    private var apiKey: String,
) {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun updateCredentials(url: String, key: String) {
        baseUrl = url.trimEnd('/')
        apiKey = key
    }

    private fun authRequest(path: String): Request.Builder =
        Request.Builder()
            .url("${baseUrl.trimEnd('/')}$path")
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")

    suspend fun health(): HealthStatus = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("${baseUrl.trimEnd('/')}/health").get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            val j = JSONObject(body.ifBlank { "{}" })
            val caps = j.optJSONArray("backend_caps")
            HealthStatus(
                ok = j.optBoolean("ok"),
                version = j.optString("version").ifBlank { null },
                backendConnected = j.optBoolean("backend_connected"),
                caps = buildList {
                    if (caps != null) for (i in 0 until caps.length()) add(caps.optString(i))
                },
            )
        }
    }

    suspend fun listModels(): List<RemoteModel> = withContext(Dispatchers.IO) {
        val req = authRequest("/v1/models").get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("models ${resp.code}: $body")
            val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
            buildList {
                for (i in 0 until data.length()) {
                    val o = data.optJSONObject(i) ?: continue
                    add(RemoteModel(id = o.optString("id"), ownedBy = o.optString("owned_by", "kaggle-bridge")))
                }
            }
        }
    }

    /** Non-streaming completion */
    suspend fun chatOnce(model: String, messages: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) {
            val payload = buildPayload(model, messages, stream = false)
            val req = authRequest("/v1/chat/completions")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext "[Error ${resp.code}] $body"
                JSONObject(body)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?: body
            }
        }

    /**
     * SSE streaming. Emits reasoning_content / content deltas.
     * Gateway may send full answer as progressive chunks even when backend is non-stream.
     */
    fun chatStream(model: String, messages: List<Pair<String, String>>): Flow<StreamEvent> = callbackFlow {
        val payload = buildPayload(model, messages, stream = true)
        val req = authRequest("/v1/chat/completions")
            .header("Accept", "text/event-stream")
            .post(payload.toRequestBody(jsonMedia))
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    trySend(StreamEvent.Done(null))
                    close()
                    return
                }
                try {
                    val j = JSONObject(data)
                    val choice = j.optJSONArray("choices")?.optJSONObject(0) ?: return
                    val delta = choice.optJSONObject("delta")
                    val msg = choice.optJSONObject("message")
                    val finish = choice.optString("finish_reason").ifBlank { null }

                    val reasoning = delta?.optString("reasoning_content")
                        ?: delta?.optString("reasoning")
                        ?: ""
                    val content = delta?.optString("content")
                        ?: msg?.optString("content")
                        ?: ""
                    // Some gateways put search progress in reasoning or a custom field
                    val progress = delta?.optString("search_progress").orEmpty()

                    if (progress.isNotBlank()) trySend(StreamEvent.SearchProgress(progress))
                    if (reasoning.isNotBlank()) trySend(StreamEvent.ReasoningDelta(reasoning))
                    if (content.isNotBlank()) trySend(StreamEvent.ContentDelta(content))
                    if (finish != null && finish != "null") {
                        trySend(StreamEvent.Done(finish))
                        close()
                    }
                } catch (e: Exception) {
                    // non-JSON keep-alive comments ignored
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val code = response?.code
                val errBody = try { response?.body?.string() } catch (_: Exception) { null }
                trySend(
                    StreamEvent.Error(
                        t?.message
                            ?: errBody
                            ?: "stream failed${code?.let { " ($it)" } ?: ""}",
                    ),
                )
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                trySend(StreamEvent.Done(null))
                close()
            }
        }

        val es = EventSources.createFactory(client).newEventSource(req, listener)
        awaitClose { es.cancel() }
    }

    /**
     * @param messages role + either plain string content or multimodal parts
     * (text + image_url data URLs). Image detail = high for max vision quality.
     */
    data class Msg(
        val role: String,
        val text: String,
        val imageDataUrls: List<String> = emptyList(),
        val visionDetail: String = "high",
    )

    private fun buildPayload(model: String, messages: List<Msg>, stream: Boolean): String {
        val arr = JSONArray()
        messages.forEach { m ->
            val content: Any = if (m.imageDataUrls.isEmpty()) {
                m.text
            } else {
                JSONArray().apply {
                    if (m.text.isNotBlank()) {
                        put(JSONObject().put("type", "text").put("text", m.text))
                    }
                    m.imageDataUrls.forEach { url ->
                        put(
                            JSONObject()
                                .put("type", "image_url")
                                .put(
                                    "image_url",
                                    JSONObject()
                                        .put("url", url)
                                        .put("detail", m.visionDetail),
                                ),
                        )
                    }
                }
            }
            arr.put(JSONObject().put("role", m.role).put("content", content))
        }
        return JSONObject()
            .put("model", model)
            .put("stream", stream)
            .put("messages", arr)
            .put("stream_options", JSONObject().put("include_usage", true))
            .toString()
    }

    private fun List<Pair<String, String>>.toMsgs(): List<Msg> =
        map { (role, text) -> Msg(role, text) }

    // Overloads used by existing call sites
    private fun buildPayload(
        model: String,
        messages: List<Pair<String, String>>,
        stream: Boolean,
    ): String = buildPayload(model, messages.toMsgs(), stream)

    fun chatStreamVision(
        model: String,
        messages: List<Msg>,
    ): Flow<StreamEvent> = callbackFlow {
        val payload = buildPayload(model, messages, stream = true)
        val req = authRequest("/v1/chat/completions")
            .header("Accept", "text/event-stream")
            .post(payload.toRequestBody(jsonMedia))
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                if (data == "[DONE]") {
                    trySend(StreamEvent.Done(null))
                    close()
                    return
                }
                try {
                    val j = JSONObject(data)
                    val choice = j.optJSONArray("choices")?.optJSONObject(0) ?: return
                    val delta = choice.optJSONObject("delta") ?: JSONObject()
                    val content = delta.optString("content")
                    val reasoning = delta.optString("reasoning_content").ifBlank {
                        delta.optString("reasoning")
                    }
                    val finish = choice.optString("finish_reason")
                    if (reasoning.isNotBlank()) trySend(StreamEvent.ReasoningDelta(reasoning))
                    if (content.isNotBlank()) trySend(StreamEvent.ContentDelta(content))
                    if (finish.isNotBlank() && finish != "null") {
                        trySend(StreamEvent.Done(finish))
                        close()
                    }
                } catch (_: Exception) {
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                trySend(StreamEvent.Error(t?.message ?: "stream failed"))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                trySend(StreamEvent.Done(null))
                close()
            }
        }
        val es = EventSources.createFactory(client).newEventSource(req, listener)
        awaitClose { es.cancel() }
    }

    suspend fun chatOnceVision(model: String, messages: List<Msg>): String =
        withContext(Dispatchers.IO) {
            val payload = buildPayload(model, messages, stream = false)
            val req = authRequest("/v1/chat/completions")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("HTTP ${resp.code}: ${body.take(400)}")
                JSONObject(body)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?: body
            }
        }
}

