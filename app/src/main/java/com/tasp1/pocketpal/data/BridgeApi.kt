package com.tasp1.pocketpal.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BridgeApi(
    private val baseUrl: String = BridgeConfig.BASE_URL,
    private val apiKey: String = BridgeConfig.API_KEY,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    data class Health(
        val ok: Boolean,
        val version: String?,
        val backendConnected: Boolean,
        val caps: List<String>,
    )

    suspend fun health(): Health = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$baseUrl/health").get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            val j = JSONObject(body)
            val caps = j.optJSONArray("backend_caps")
            Health(
                ok = j.optBoolean("ok"),
                version = j.optString("version"),
                backendConnected = j.optBoolean("backend_connected"),
                caps = buildList {
                    if (caps != null) for (i in 0 until caps.length()) add(caps.getString(i))
                },
            )
        }
    }

    suspend fun chat(model: String, userText: String): String = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("model", model)
            .put("stream", false)
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", userText)),
            )
        val req = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) return@withContext "[Error ${resp.code}] $body"
            val j = JSONObject(body)
            j.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?: body
        }
    }
}
