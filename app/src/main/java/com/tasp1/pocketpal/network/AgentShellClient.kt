package com.tasp1.pocketpal.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Native agent shell against TASP1 Render / Kaggle Bridge endpoints.
 *
 * Preferred paths (tried in order):
 *  - POST {base}/shell/exec   { "command": "..." }  Authorization: Bearer <key>
 *  - POST {base}/api/shell    same body
 *  - POST {base}/v1/shell     same body
 *
 * Also probes Render-hosted MCP/gh-cli services when configured.
 */
class AgentShellClient(
    private var baseUrl: String,
    private var apiKey: String,
) {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun updateCredentials(url: String, key: String) {
        baseUrl = url.trimEnd('/')
        apiKey = key
    }

    data class ShellResult(
        val ok: Boolean,
        val stdout: String,
        val stderr: String = "",
        val exitCode: Int? = null,
        val raw: String = "",
        val endpoint: String = "",
    )

    suspend fun exec(command: String, cwd: String? = null): ShellResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("command", command)
            .put("cmd", command)
            .also { if (cwd != null) it.put("cwd", cwd) }
            .toString()
            .toRequestBody(jsonMedia)

        val paths = listOf("/shell/exec", "/api/shell", "/v1/shell", "/mcp/shell")
        var lastErr = "no endpoint responded"
        for (path in paths) {
            val req = Request.Builder()
                .url("${baseUrl.trimEnd('/')}$path")
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .post(body)
                .build()
            try {
                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        return@withContext parse(text, path)
                    }
                    lastErr = "HTTP ${resp.code} $path: ${text.take(200)}"
                }
            } catch (e: Exception) {
                lastErr = "${e.message} @ $path"
            }
        }
        ShellResult(ok = false, stdout = "", stderr = lastErr)
    }

    /** Best-effort health for shell capability */
    suspend fun probe(): Boolean = withContext(Dispatchers.IO) {
        for (path in listOf("/health", "/api/health", "/v1/models", "/shell/exec")) {
            val req = Request.Builder()
                .url("${baseUrl.trimEnd('/')}$path")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            try {
                client.newCall(req).execute().use { resp ->
                    if (resp.code in 200..499) return@withContext true
                }
            } catch (_: Exception) {
            }
        }
        false
    }

    private fun parse(text: String, path: String): ShellResult {
        return try {
            val o = JSONObject(text)
            val stdout = sequenceOf("stdout", "output", "result", "message")
                .mapNotNull { k -> o.optString(k).takeIf { it.isNotBlank() } }
                .firstOrNull()
                ?: o.optJSONObject("data")?.optString("stdout").orEmpty()
            val stderr = o.optString("stderr")
            val code = when {
                o.has("exit_code") -> o.optInt("exit_code")
                o.has("returncode") -> o.optInt("returncode")
                o.has("code") -> o.optInt("code")
                else -> null
            }
            val ok = code == null || code == 0
            ShellResult(ok = ok, stdout = stdout.ifBlank { text }, stderr = stderr, exitCode = code, raw = text, endpoint = path)
        } catch (_: Exception) {
            ShellResult(ok = true, stdout = text, endpoint = path, raw = text)
        }
    }
}
