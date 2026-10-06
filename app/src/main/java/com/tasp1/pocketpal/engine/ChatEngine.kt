package com.tasp1.pocketpal.engine

import com.tasp1.pocketpal.domain.HealthStatus
import com.tasp1.pocketpal.domain.RemoteModel
import com.tasp1.pocketpal.domain.ServerProfile
import com.tasp1.pocketpal.network.BridgeClient
import com.tasp1.pocketpal.protocol.StreamEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Unified engine surface — remote OpenAI-compatible, Kaggle Bridge, or local GGUF.
 * ChatViewModel talks only to ChatEngine so backends can be swapped without UI churn.
 */
interface ChatEngine {
    val profile: ServerProfile
    suspend fun health(): HealthStatus
    suspend fun listModels(): List<RemoteModel>
    fun stream(model: String, messages: List<BridgeClient.Msg>): Flow<StreamEvent>
    suspend fun complete(model: String, messages: List<BridgeClient.Msg>): String
    fun supportsVision(): Boolean
    fun supportsNativeFlags(): Boolean // :web :think :shell
}

class RemoteOpenAIEngine(
    override val profile: ServerProfile,
    private val client: BridgeClient = BridgeClient(profile.baseUrl, profile.apiKey),
) : ChatEngine {
    fun updateAuth(url: String, key: String) = client.updateCredentials(url, key)

    override suspend fun health(): HealthStatus = runCatching {
        client.health()
    }.getOrElse {
        // Non-bridge OpenAI endpoints often lack /health — probe models
        runCatching {
            client.listModels()
            HealthStatus(ok = true, version = profile.kind.name, backendConnected = true, caps = listOf("stream"))
        }.getOrElse {
            HealthStatus(ok = false, version = null, backendConnected = false, caps = emptyList())
        }
    }

    override suspend fun listModels(): List<RemoteModel> = runCatching { client.listModels() }.getOrDefault(emptyList())

    override fun stream(model: String, messages: List<BridgeClient.Msg>): Flow<StreamEvent> =
        client.chatStreamVision(model, messages)

    override suspend fun complete(model: String, messages: List<BridgeClient.Msg>): String =
        client.chatOnceVision(model, messages)

    override fun supportsVision(): Boolean = true
    override fun supportsNativeFlags(): Boolean = profile.kind == ServerProfile.Kind.KaggleBridge
}

/**
 * Local GGUF engine shell — ready for llama.cpp / MediaPipe binding.
 * Keeps architecture in place; inference returns a clear not-linked message until native lib is wired.
 */
class LocalGgufEngine(
    override val profile: ServerProfile = ServerProfile.local(),
) : ChatEngine {
    @Volatile var loadedModelPath: String? = null
    @Volatile var isReady: Boolean = false

    override suspend fun health() = HealthStatus(
        ok = isReady,
        version = "local-gguf-0.1",
        backendConnected = isReady,
        caps = if (isReady) listOf("local", "stream") else emptyList(),
    )

    override suspend fun listModels(): List<RemoteModel> {
        val path = loadedModelPath ?: return emptyList()
        return listOf(RemoteModel(id = path, ownedBy = "local"))
    }

    override fun stream(model: String, messages: List<BridgeClient.Msg>): Flow<StreamEvent> = flow {
        if (!isReady) {
            emit(StreamEvent.Error(
                "Local GGUF engine not linked yet. Place a .gguf model and enable On-device in Models. " +
                    "Architecture is ready for llama.cpp / ExecuTorch binding.",
            ))
            emit(StreamEvent.Done("error"))
            return@flow
        }
        emit(StreamEvent.ContentDelta("Local inference placeholder for $model."))
        emit(StreamEvent.Done("stop"))
    }

    override suspend fun complete(model: String, messages: List<BridgeClient.Msg>): String {
        if (!isReady) return "[Local] GGUF runtime not linked — use Kaggle Bridge or another remote server for now."
        return "Local completion placeholder."
    }

    override fun supportsVision(): Boolean = false
    override fun supportsNativeFlags(): Boolean = false

    fun markModelLoaded(path: String) {
        loadedModelPath = path
        isReady = true
    }

    fun unload() {
        loadedModelPath = null
        isReady = false
    }
}

/** Routes to active profile engine */
class EngineRouter(
    private var remote: RemoteOpenAIEngine,
    val local: LocalGgufEngine = LocalGgufEngine(),
) {
    var useLocal: Boolean = false

    val active: ChatEngine get() = if (useLocal) local else remote

    fun setRemote(profile: ServerProfile) {
        remote = RemoteOpenAIEngine(profile)
        useLocal = profile.kind == ServerProfile.Kind.LocalGguf
    }

    fun updateRemoteAuth(url: String, key: String) {
        remote.updateAuth(url, key)
    }
}
