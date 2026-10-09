package com.tasp1.pocketpal.ui.screens.chat

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tasp1.pocketpal.PocketPalApp
import com.tasp1.pocketpal.data.AttachmentProcessor
import com.tasp1.pocketpal.ui.chat.ChatViewModel
import com.tasp1.pocketpal.ui.components.ChatBubble
import com.tasp1.pocketpal.ui.components.ChatEmptyPlaceholder
import com.tasp1.pocketpal.ui.components.ChatInputBar
import com.tasp1.pocketpal.ui.components.ChatTopBar
import com.tasp1.pocketpal.ui.components.claude.ScrollToBottomFab
import com.tasp1.pocketpal.ui.components.sheets.AddToChatSheet
import com.tasp1.pocketpal.ui.components.sheets.ModelOption
import com.tasp1.pocketpal.ui.components.sheets.ModelPickerSheet
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(onOpenDrawer: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as PocketPalApp
    val vm: ChatViewModel = viewModel(factory = ChatViewModel.Factory(app.container))
    val state by vm.ui.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showModels by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    val showScrollFab by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) false
            else {
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                last < total - 1
            }
        }
    }

    val multiImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8),
    ) { uris ->
        if (uris.isNotEmpty()) vm.addAttachments(uris)
    }
    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) vm.addAttachments(uris)
    }

    LaunchedEffect(state.turns.size, state.turns.lastOrNull()?.content?.length) {
        if (state.turns.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(state.turns.lastIndex) }
        }
    }
    LaunchedEffect(state.attachError) {
        state.attachError?.let {
            snack.showSnackbar(it)
            vm.clearAttachError()
        }
    }

    val modelLabel = vm.modelId()
    val healthHint = state.health?.let {
        when {
            !it.ok -> "gateway down"
            !it.backendConnected -> "notebook offline"
            else -> "v${it.version ?: "?"}"
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                title = state.title,
                modelId = modelLabel + (healthHint?.let { " · $it" } ?: ""),
                onMenu = onOpenDrawer,
                onNewChat = { vm.newChat() },
                onDeleteChat = { vm.deleteCurrentChat() },
                onShare = {
                    val text = state.turns.joinToString("\n\n") { turn ->
                        val who = if (turn.role.name == "User") "You" else "Assistant"
                        "$who: ${turn.content}"
                    }
                    if (text.isNotBlank()) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        runCatching {
                            ctx.startActivity(Intent.createChooser(send, "Share chat"))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            ChatInputBar(
                value = state.input,
                onValueChange = vm::setInput,
                webEnabled = state.flags.web,
                thinkEnabled = state.flags.think,
                shellEnabled = state.flags.shell,
                onToggleWeb = vm::toggleWeb,
                onToggleThink = vm::toggleThink,
                onToggleShell = vm::toggleShell,
                onSend = vm::send,
                onStop = vm::stopGeneration,
                modelLabel = state.baseModel.substringAfterLast('/').take(22),
                onModelClick = { showModels = true },
                sending = state.sending,
                pending = state.pending,
                onAttach = { showAdd = true },
                onRemovePending = vm::removePending,
                processingAttach = state.processingAttach,
                highVision = state.visionQuality == AttachmentProcessor.VisionQuality.High,
                onToggleHighVision = {
                    vm.setVisionQuality(
                        if (state.visionQuality == AttachmentProcessor.VisionQuality.High)
                            AttachmentProcessor.VisionQuality.Balanced
                        else AttachmentProcessor.VisionQuality.High,
                    )
                },
                autoOcr = state.autoOcr,
                onToggleOcr = { vm.setAutoOcr(!state.autoOcr) },
                modifier = Modifier.imePadding(),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.turns.isEmpty()) {
                ChatEmptyPlaceholder(modelId = modelLabel)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.turns, key = { it.id }) { turn ->
                        ChatBubble(turn)
                    }
                }
            }
            if (showScrollFab) {
                ScrollToBottomFab(
                    onClick = {
                        scope.launch {
                            runCatching { listState.animateScrollToItem(state.turns.lastIndex.coerceAtLeast(0)) }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }
        }
    }

    if (showModels) {
        ModelPickerSheet(
            models = listOf(
                ModelOption("google/gemini-2.5-flash", "Gemini 2.5 Flash", "Fast · everyday"),
                ModelOption("google/gemini-2.5-pro", "Gemini 2.5 Pro", "Stronger reasoning"),
                ModelOption("anthropic/claude-sonnet-5@default", "Claude Sonnet", "Balanced chat"),
                ModelOption("anthropic/claude-opus-4-1-20250805", "Claude Opus", "Hardest tasks"),
                ModelOption("openai/gpt-5.4-mini-2026-03-17", "GPT 5.4 Mini", "Quick answers"),
                ModelOption("deepseek-ai/deepseek-r1-0528", "DeepSeek R1", "Deep reasoning"),
                ModelOption("qwen/qwen3-235b-a22b-instruct-2507", "Qwen3 235B", "Large instruct"),
                ModelOption("xai/grok-4", "Grok 4", "xAI"),
            ),
            selectedId = state.baseModel,
            onSelect = { id ->
                vm.setBaseModel(id)
                showModels = false
            },
            onDismiss = { showModels = false },
            effortLabel = if (state.flags.think) "High" else "Medium",
        )
    }
    if (showAdd) {
        AddToChatSheet(
            webSearch = state.flags.web,
            onWebSearch = { on -> if (on != state.flags.web) vm.toggleWeb() },
            onCamera = {
                showAdd = false
                multiImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onPhotos = {
                showAdd = false
                multiImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onFiles = {
                showAdd = false
                openDoc.launch(arrayOf("*/*"))
            },
            onDismiss = { showAdd = false },
        )
    }
}
