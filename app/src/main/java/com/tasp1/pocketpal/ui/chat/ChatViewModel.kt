package com.tasp1.pocketpal.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tasp1.pocketpal.data.AppContainer
import com.tasp1.pocketpal.core.CrashGuard
import com.tasp1.pocketpal.data.AttachmentProcessor
import com.tasp1.pocketpal.data.BridgeContent
import com.tasp1.pocketpal.data.MessageCleaner
import com.tasp1.pocketpal.domain.Attachment
import com.tasp1.pocketpal.domain.ChatTurn
import com.tasp1.pocketpal.domain.ChatSession
import com.tasp1.pocketpal.domain.HealthStatus
import com.tasp1.pocketpal.network.BridgeClient
import com.tasp1.pocketpal.protocol.ModelFlags
import com.tasp1.pocketpal.protocol.StreamEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.plus
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val sessionId: String = "",
    val turns: List<ChatTurn> = emptyList(),
    val title: String = "Hello",
    val flags: ModelFlags = ModelFlags(web = true),
    val baseModel: String = "google/gemini-2.5-flash",
    val input: String = "",
    val sending: Boolean = false,
    val health: HealthStatus? = null,
    val useStream: Boolean = true,
    val pending: List<Attachment> = emptyList(),
    val visionQuality: AttachmentProcessor.VisionQuality = AttachmentProcessor.VisionQuality.High,
    val autoOcr: Boolean = true,
    val attachError: String? = null,
    val processingAttach: Boolean = false,
)

class ChatViewModel(private val container: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(ChatUiState())
    val ui: StateFlow<ChatUiState> = _ui.asStateFlow()
    private var streamJob: Job? = null
    private var currentSessionId: String = container.sessions.newSessionId()

    init {
        _ui.update { it.copy(sessionId = currentSessionId) }

        viewModelScope.launch {
            container.settings.settings.collect { s ->
                container.applyServer(s.serverUrl, s.apiKey, useLocal = s.useLocal)
                _ui.update { it.copy(baseModel = s.baseModel) }
            }
        }
        refreshHealth()
    }

    fun refreshHealth() {
        viewModelScope.launch {
            val h = runCatching { container.engines.active.health() }.getOrNull()
            _ui.update { it.copy(health = h) }
        }
    }

    fun setInput(v: String) = _ui.update { it.copy(input = v) }

    fun clearAttachError() = _ui.update { it.copy(attachError = null) }

    fun setVisionQuality(q: AttachmentProcessor.VisionQuality) =
        _ui.update { it.copy(visionQuality = q) }

    fun setAutoOcr(v: Boolean) = _ui.update { it.copy(autoOcr = v) }

    fun toggleWeb() = _ui.update {
        val w = !it.flags.web
        it.copy(flags = it.flags.copy(web = w, shell = if (w) false else it.flags.shell))
    }

    fun toggleThink() = _ui.update {
        it.copy(flags = it.flags.copy(think = !it.flags.think, effort = null))
    }

    fun toggleShell() = _ui.update {
        val s = !it.flags.shell
        it.copy(flags = it.flags.copy(shell = s, web = if (s) false else it.flags.web))
    }

    fun newChat() {
        streamJob?.cancel()
        currentSessionId = container.sessions.newSessionId()
        _ui.update {
            it.copy(
                sessionId = currentSessionId,
                turns = emptyList(),
                title = "New chat",
                sending = false,
                pending = emptyList(),
            )
        }
    }

    fun setBaseModel(id: String) {
        viewModelScope.launch {
            container.settings.setBaseModel(id)
            _ui.update { it.copy(baseModel = id) }
        }
    }

    fun modelId(): String = _ui.value.flags.toModelId(_ui.value.baseModel)

    fun addAttachments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _ui.update { it.copy(processingAttach = true, attachError = null) }
            val quality = _ui.value.visionQuality
            val ocr = _ui.value.autoOcr
            val loaded = mutableListOf<Attachment>()
            var err: String? = null
            for (uri in uris) {
                try {
                    loaded += container.attachments.fromUri(uri, quality, runOcr = ocr)
                } catch (e: Exception) {
                    err = e.message ?: "Failed to load attachment"
                }
            }
            _ui.update {
                it.copy(
                    pending = it.pending + loaded,
                    processingAttach = false,
                    attachError = err,
                )
            }
        }
    }

    fun removePending(id: String) {
        _ui.update { it.copy(pending = it.pending.filterNot { a -> a.id == id }) }
    }

    fun clearPending() = _ui.update { it.copy(pending = emptyList()) }

    private suspend fun applyServerFromSettings() {
        runCatching {
            val s = container.settings.settings.first()
            container.applyServer(s.serverUrl, s.apiKey, useLocal = false)
        }
    }

    fun send() {
        val state = _ui.value
        val text = state.input.trim()
        val pending = state.pending
        if ((text.isEmpty() && pending.isEmpty()) || state.sending) return
        // Agent shortcuts: `$ cmd` or `/shell cmd` → native shell endpoint
        if (pending.isEmpty() && (text.startsWith("$ ") || text.startsWith("/shell "))) {
            val cmd = text.removePrefix("$ ").removePrefix("/shell ").trim()
            _ui.update { it.copy(input = "") }
            runShell(cmd, useRenderAgent = state.flags.shell)
            return
        }

        // Never attach :shell for normal chat — shell is only via `$ cmd` / runShell
        val model = state.flags.copy(shell = false).toModelId(state.baseModel)
        streamJob?.cancel()
        _ui.update { st ->
            st.copy(
                turns = st.turns.map {
                    when {
                        it.isStreaming && it.content.isBlank() ->
                            it.copy(isStreaming = false, content = "")
                        it.isStreaming -> it.copy(isStreaming = false)
                        else -> it
                    }
                },
            )
        }
        val userId = UUID.randomUUID().toString()
        val asstId = UUID.randomUUID().toString()

        val ocrBlocks = pending.mapNotNull { a ->
            a.ocrText?.takeIf { it.isNotBlank() }?.let { ocr ->
                "\n\n[OCR: ${a.displayName}]\n$ocr"
            }
        }.joinToString("")
        val userText = buildString {
            append(text.ifBlank {
                if (pending.any { it.isImage }) "Describe the image(s) in detail."
                else "Review the attached file(s)."
            })
            if (ocrBlocks.isNotBlank()) {
                append("\n\n--- Extracted text (on-device OCR) ---")
                append(ocrBlocks)
            }
        }
        val images = pending.mapNotNull { it.dataUrl }
        val names = pending.map { it.displayName }

        // CRITICAL: do NOT put multi-MB base64 data-URLs into Compose state (OOM / crash on send).
        // Network payload still uses `images` below.
        val userTurn = ChatTurn(
            id = userId,
            role = ChatTurn.Role.User,
            content = text.ifBlank { if (names.isNotEmpty()) "(${names.size} attachment)" else "(attachment)" },
            imageDataUrls = emptyList(),
            attachmentNames = names,
            ocrPreview = pending.mapNotNull { it.ocrText }.firstOrNull()?.take(120),
        )
        val asstTurn = ChatTurn(
            id = asstId,
            role = ChatTurn.Role.Assistant,
            content = "",
            isStreaming = true,
        )

        _ui.update {
            it.copy(
                input = "",
                pending = emptyList(),
                sending = true,
                title = if (it.title == "Hello" || it.title == "New chat") {
                    (text.ifBlank { names.firstOrNull() ?: "Attachment" }).take(40)
                } else it.title,
                turns = it.turns + userTurn + asstTurn,
            )
        }

        val historyMsgs = buildHistoryMsgs(userText, images)
        val handler = CoroutineExceptionHandler { _, e ->
            if (e is CancellationException) return@CoroutineExceptionHandler
            android.util.Log.e("ChatVM", "send.handler", e)
            _ui.update { st ->
                st.copy(
                    sending = false,
                    turns = st.turns.map {
                        if (it.id == asstId) it.copy(
                            isStreaming = false,
                            error = (e.message ?: "send failed").take(300),
                            content = it.content.ifBlank { "" },
                        ) else it
                    },
                )
            }
            persistSession()
        }
        // Cancel previous without marking _(stopped)_ — that text is only for user Stop
        streamJob?.cancel()
        streamJob = viewModelScope.launch(handler) {
            try {
                streamOrOnce(model, historyMsgs, asstId)
            } catch (ce: CancellationException) {
                // previous job cancelled or user stop — leave stopGeneration to set UI if needed
                _ui.update { st ->
                    st.copy(
                        sending = false,
                        turns = st.turns.map {
                            if (it.id == asstId && it.isStreaming && it.content.isBlank())
                                it.copy(isStreaming = false, content = "")
                            else if (it.id == asstId && it.isStreaming)
                                it.copy(isStreaming = false)
                            else it
                        },
                    )
                }
            } catch (t: Throwable) {
                android.util.Log.e("ChatVM", "send.body", t)
                patchAssistant(asstId, "", "", false, (t.message ?: "Send failed").take(300))
                _ui.update { it.copy(sending = false) }
                persistSession()
            }
        }
    }

    private fun buildHistoryMsgs(
        lastUserText: String,
        lastImages: List<String>,
    ): List<BridgeClient.Msg> {
        val prior = _ui.value.turns.dropLast(1) // exclude empty streaming assistant; includes user
        // Rebuild from completed turns only (no streaming)
        val msgs = mutableListOf<BridgeClient.Msg>()
        // Use current turns before we added the empty assistant — already in state with user+asst
        val turns = _ui.value.turns.filter { !it.isStreaming || it.role == ChatTurn.Role.User }
        // Simpler: walk turns except last assistant placeholder
        val stable = _ui.value.turns.dropLast(1)
        stable.forEach { t ->
            when (t.role) {
                ChatTurn.Role.User -> msgs.add(
                    BridgeClient.Msg(
                        role = "user",
                        text = t.content,
                        imageDataUrls = t.imageDataUrls,
                        visionDetail = "high",
                    ),
                )
                ChatTurn.Role.Assistant -> msgs.add(
                    BridgeClient.Msg(role = "assistant", text = t.content),
                )
            }
        }
        // Last user already in stable; if images were only on last, ensure text includes OCR path
        if (msgs.isEmpty() || msgs.last().role != "user") {
            msgs.add(
                BridgeClient.Msg(
                    role = "user",
                    text = lastUserText,
                    imageDataUrls = lastImages.take(4),  // hard cap vision images
                    visionDetail = "high",
                ),
            )
        } else {
            // Replace last user with enriched text + images
            msgs[msgs.lastIndex] = BridgeClient.Msg(
                role = "user",
                text = lastUserText,
                imageDataUrls = lastImages.take(4),  // hard cap vision images
                visionDetail = "high",
            )
        }
        return msgs
    }

    private suspend fun streamOrOnce(
        model: String,
        history: List<BridgeClient.Msg>,
        asstId: String,
    ) {
        applyServerFromSettings()
        val content = StringBuilder()
        val reasoning = StringBuilder()
        var finished = false
        var lastUiMs = 0L

        fun pushUi(force: Boolean = false) {
            val now = System.currentTimeMillis()
            if (!force && now - lastUiMs < 80) return
            lastUiMs = now
            val vis = runCatching { MessageCleaner.visibleBody(content.toString()) }.getOrDefault(content.toString())
            val r = runCatching {
                MessageCleaner.visibleReasoning(content.toString(), reasoning.toString())
            }.getOrDefault(reasoning.toString())
            patchAssistant(asstId, vis, r, true)
        }

        // 1) Prefer non-stream complete — reliable on mobile networks
        try {
            val raw = container.engines.active.complete(model, history)
            if (raw.isNotBlank() && !raw.startsWith("[Error") && !raw.startsWith("[Local]")) {
                finalizeAssistant(asstId, raw, "")
                return
            }
            // fall through to stream if empty/error response
            if (raw.startsWith("[Error")) {
                android.util.Log.w("ChatVM", "complete error: $raw")
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            android.util.Log.w("ChatVM", "complete failed, trying stream", t)
        }

        // 2) Streaming fallback
        try {
            container.engines.active.stream(model, history)
                .flowOn(Dispatchers.IO)
                .collect { ev ->
                    try {
                        when (ev) {
                            is StreamEvent.ContentDelta -> {
                                content.append(ev.text)
                                pushUi()
                            }
                            is StreamEvent.ReasoningDelta -> {
                                reasoning.append(ev.text)
                                pushUi()
                            }
                            is StreamEvent.SearchProgress -> {
                                val msg = ev.text.trim()
                                if (msg.isBlank()) return@collect
                                _ui.update { st ->
                                    st.copy(
                                        turns = st.turns.map { turn ->
                                            if (turn.id != asstId) turn
                                            else {
                                                val steps = turn.toolSteps.toMutableList()
                                                val last = steps.lastOrNull()
                                                if (last != null && !last.done &&
                                                    last.kind == com.tasp1.pocketpal.domain.ToolStep.Kind.Search
                                                ) {
                                                    steps[steps.lastIndex] = last.copy(title = msg.take(80))
                                                } else {
                                                    steps += com.tasp1.pocketpal.domain.ToolStep(
                                                        id = java.util.UUID.randomUUID().toString(),
                                                        title = msg.take(80),
                                                        kind = com.tasp1.pocketpal.domain.ToolStep.Kind.Search,
                                                        done = false,
                                                    )
                                                }
                                                turn.copy(toolSteps = steps)
                                            }
                                        },
                                    )
                                }
                            }
                            is StreamEvent.Done -> {
                                finished = true
                                pushUi(force = true)
                                finalizeAssistant(asstId, content.toString(), reasoning.toString())
                            }
                            is StreamEvent.Error -> {
                                finished = true
                                if (content.isEmpty()) {
                                    // last resort complete already tried — show error in bubble
                                    patchAssistant(
                                        asstId,
                                        "",
                                        "",
                                        false,
                                        error = ev.message.take(300),
                                    )
                                    _ui.update { it.copy(sending = false) }
                                    persistSession()
                                } else {
                                    finalizeAssistant(asstId, content.toString(), reasoning.toString())
                                }
                            }
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (t: Throwable) {
                        // Log only — never toast spam mid-stream
                        android.util.Log.e("ChatVM", "stream.event", t)
                    }
                }
            if (!finished) {
                if (content.isNotEmpty() || reasoning.isNotEmpty()) {
                    finalizeAssistant(asstId, content.toString(), reasoning.toString())
                } else {
                    patchAssistant(asstId, "", "", false, error = "No response from server")
                    _ui.update { it.copy(sending = false) }
                    persistSession()
                }
            }
        } catch (ce: CancellationException) {
            // User stopped or new message started — do not toast
            throw ce
        } catch (t: Throwable) {
            android.util.Log.e("ChatVM", "streamOrOnce failed", t)
            if (content.isNotEmpty()) {
                finalizeAssistant(asstId, content.toString(), reasoning.toString())
            } else {
                patchAssistant(asstId, "", "", false, error = (t.message ?: "Request failed").take(300))
                _ui.update { it.copy(sending = false) }
                persistSession()
            }
        }
    }

    private fun finalizeAssistant(asstId: String, rawContent: String, rawReasoning: String) {
        try {
            val cleaned = MessageCleaner.clean(rawContent, rawReasoning)
            _ui.update { st ->
                st.copy(
                    sending = false,
                    turns = st.turns.map {
                        if (it.id == asstId) it.copy(
                            content = cleaned.body.ifBlank { "(empty response)" },
                            reasoning = cleaned.reasoning,
                            isStreaming = false,
                            sourcesJson = cleaned.sources.joinToString("\n") { s ->
                                "${s.index}|${s.title}|${s.url}"
                            },
                            error = null,
                        ) else it
                    },
                )
            }
            persistSession()
        } catch (e: Exception) {
            android.util.Log.e("ChatVM", "finalize failed", e)
            patchAssistant(asstId, MessageCleaner.visibleBody(rawContent).ifBlank { "(error)" }, rawReasoning, false, e.message)
            _ui.update { it.copy(sending = false) }
            persistSession()
        }
    }

    private suspend fun runOnce(
        model: String,
        history: List<BridgeClient.Msg>,
        asstId: String,
    ) {
        try {
            val raw = container.engines.active.complete(model, history)
            val cleaned = MessageCleaner.clean(raw)
            _ui.update { st ->
                st.copy(
                    sending = false,
                    turns = st.turns.map {
                        if (it.id == asstId) it.copy(
                            content = cleaned.body.ifBlank { "(empty response)" },
                            reasoning = cleaned.reasoning.ifBlank { it.reasoning },
                            isStreaming = false,
                            sourcesJson = cleaned.sources.joinToString("\n") { s ->
                                "${s.index}|${s.title}|${s.url}"
                            },
                            error = null,
                        ) else it
                    },
                )
            }
            persistSession()
        } catch (e: Exception) {
            android.util.Log.e("ChatVM", "runOnce failed", e)
            patchAssistant(asstId, "", "", false, error = e.message ?: "request failed")
            _ui.update { it.copy(sending = false) }
            persistSession()
        }
    }

    private fun patchAssistant(
        id: String,
        content: String,
        reasoning: String,
        streaming: Boolean,
        error: String? = null,
    ) {
        _ui.update { st ->
            st.copy(
                turns = st.turns.map {
                    if (it.id == id) it.copy(
                        content = content,
                        reasoning = reasoning,
                        isStreaming = streaming,
                        error = error, // null clears previous error
                    ) else it
                },
            )
        }
    }

    private fun persistSession() {
        viewModelScope.launch {
            runCatching {
                val st = _ui.value
                val id = currentSessionId.ifBlank {
                    container.sessions.newSessionId().also { currentSessionId = it }
                }
                val sess = ChatSession(
                    id = id,
                    title = st.title.ifBlank { "Chat" },
                    updatedAt = System.currentTimeMillis(),
                    modelId = runCatching { modelId() }.getOrDefault(st.baseModel),
                )
                container.sessions.save(sess, st.turns.filter { !it.isStreaming })
            }.onFailure { android.util.Log.w("ChatVM", "persist failed", it) }
        }
    }

    fun loadSession(id: String) {
        streamJob?.cancel()
        viewModelScope.launch {
            val payload = container.sessions.load(id) ?: return@launch
            currentSessionId = id
            val turns = payload.turns.map { t ->
                ChatTurn(
                    id = t.id,
                    role = if (t.role == "user") ChatTurn.Role.User else ChatTurn.Role.Assistant,
                    content = t.content,
                    reasoning = t.reasoning,
                    sourcesJson = t.sourcesJson,
                )
            }
            _ui.update {
                it.copy(sessionId = id, turns = turns, title = payload.session.title, sending = false)
            }
        }
    }

    
    /**
     * Native agent: run a shell command on the bridge / Render agent host.
     * Results are appended as an assistant tool step + body.
     */
    fun runShell(command: String, useRenderAgent: Boolean = false) {
        val cmd = command.trim()
        if (cmd.isEmpty() || _ui.value.sending) return
        val asstId = java.util.UUID.randomUUID().toString()
        val userId = java.util.UUID.randomUUID().toString()
        val userTurn = ChatTurn(id = userId, role = ChatTurn.Role.User, content = "$ " + cmd)
        val asstTurn = ChatTurn(
            id = asstId,
            role = ChatTurn.Role.Assistant,
            content = "",
            isStreaming = true,
            toolSteps = listOf(
                com.tasp1.pocketpal.domain.ToolStep(
                    id = java.util.UUID.randomUUID().toString(),
                    title = "Run shell: ${cmd.take(48)}",
                    kind = com.tasp1.pocketpal.domain.ToolStep.Kind.Shell,
                    request = cmd,
                    done = false,
                ),
            ),
        )
        _ui.update {
            it.copy(
                turns = it.turns + userTurn + asstTurn,
                sending = true,
                title = if (it.title == "Hello" || it.title == "New chat") cmd.take(40) else it.title,
            )
        }
        viewModelScope.launch {
            val client = if (useRenderAgent) container.renderAgent else container.shell
            val result = runCatching { client.exec(cmd) }.getOrElse {
                com.tasp1.pocketpal.network.AgentShellClient.ShellResult(
                    ok = false, stdout = "", stderr = it.message ?: "shell failed",
                )
            }
            val body = buildString {
                if (result.stdout.isNotBlank()) append(result.stdout)
                if (result.stderr.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(result.stderr)
                }
                if (isEmpty()) append("(no output)")
            }
            val step = com.tasp1.pocketpal.domain.ToolStep(
                id = java.util.UUID.randomUUID().toString(),
                title = if (result.ok) "Shell ok" else "Shell error",
                kind = com.tasp1.pocketpal.domain.ToolStep.Kind.Shell,
                request = cmd,
                response = body.take(8000),
                done = true,
            )
            _ui.update { st ->
                st.copy(
                    sending = false,
                    turns = st.turns.map {
                        if (it.id == asstId) it.copy(
                            content = "```shell\n$body\n```",
                            isStreaming = false,
                            toolSteps = listOf(step),
                            error = if (!result.ok) result.stderr.ifBlank { "exit ${result.exitCode}" } else null,
                        ) else it
                    },
                )
            }
            persistSession()
        }
    }

    fun deleteCurrentChat() {
        streamJob?.cancel()
        val id = currentSessionId
        viewModelScope.launch {
            runCatching { container.sessions.delete(id) }
        }
        newChat()
    }

    fun stopGeneration() {
        streamJob?.cancel()
        streamJob = null
        _ui.update { st ->
            st.copy(
                sending = false,
                turns = st.turns.map {
                    if (it.isStreaming) it.copy(isStreaming = false, content = it.content.ifBlank { "Stopped" }, error = null)
                    else it
                },
            )
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(container) as T
    }
}
