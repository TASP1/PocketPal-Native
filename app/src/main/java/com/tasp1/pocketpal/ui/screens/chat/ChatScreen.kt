package com.tasp1.pocketpal.ui.screens.chat

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

@Composable
fun ChatScreen(onOpenDrawer: () -> Unit) {
    val app = LocalContext.current.applicationContext as PocketPalApp
    val vm: ChatViewModel = viewModel(factory = ChatViewModel.Factory(app.container))
    val state by vm.ui.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snack = remember { SnackbarHostState() }

    // Multi image (Photo Picker — no storage permission on modern Android)
    val multiImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8),
    ) { uris ->
        if (uris.isNotEmpty()) vm.addAttachments(uris)
    }
    // Any file (large docs / PDF)
    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) vm.addAttachments(uris)
    }

    LaunchedEffect(state.turns.size, state.turns.lastOrNull()?.content?.length) {
        if (state.turns.isNotEmpty()) listState.animateScrollToItem(state.turns.lastIndex)
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
            else -> "v${it.version ?: "?"} · ${it.caps.joinToString()}"
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                title = state.title,
                modelId = modelLabel + (healthHint?.let { " · $it" } ?: ""),
                onMenu = onOpenDrawer,
                onNewChat = vm::newChat,
                onMore = { vm.refreshHealth() },
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
                pending = state.pending,
                processingAttach = state.processingAttach,
                onAttach = {
                    // Prefer images; long-press alternative would open docs — use images first
                    multiImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                    // Also allow files via second path: openDoc for PDFs — expose via same + for now
                    // Users can use system picker if we open documents — dual: open images primarily
                },
                onRemovePending = vm::removePending,
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
                onSend = vm::send,
                onStop = vm::stopGeneration,
                sending = state.sending,
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
            if (state.sending) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
            }
        }
    }
}
