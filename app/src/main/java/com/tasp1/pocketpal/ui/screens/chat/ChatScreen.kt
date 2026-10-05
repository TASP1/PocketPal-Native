package com.tasp1.pocketpal.ui.screens.chat

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tasp1.pocketpal.data.AppState
import com.tasp1.pocketpal.data.BridgeApi
import com.tasp1.pocketpal.ui.components.ChatBubble
import com.tasp1.pocketpal.ui.components.ChatEmptyPlaceholder
import com.tasp1.pocketpal.ui.components.ChatInputBar
import com.tasp1.pocketpal.ui.components.ChatMessage
import com.tasp1.pocketpal.ui.components.ChatTopBar
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ChatScreen(onOpenDrawer: () -> Unit) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var web by remember { mutableStateOf(true) }
    var think by remember { mutableStateOf(false) }
    var shell by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("Hello") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }

    fun modelId(): String {
        val base = AppState.baseModel
        val flags = buildList {
            if (web) add("web")
            if (think) add("think")
            if (shell) add("shell")
        }
        return if (flags.isEmpty()) base else "$base:${flags.joinToString(":")}"
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                title = title,
                modelId = modelId(),
                onMenu = onOpenDrawer,
                onNewChat = {
                    messages.clear()
                    title = "New chat"
                },
                onMore = {},
            )
        },
        bottomBar = {
            ChatInputBar(
                value = input,
                onValueChange = { input = it },
                webEnabled = web,
                thinkEnabled = think,
                shellEnabled = shell,
                onToggleWeb = {
                    web = !web
                    if (web) shell = false
                },
                onToggleThink = { think = !think },
                onToggleShell = {
                    shell = !shell
                    if (shell) web = false
                },
                onSend = {
                    val text = input.trim()
                    if (text.isEmpty() || sending) return@ChatInputBar
                    input = ""
                    if (title == "New chat" || title == "Hello") title = text.take(40)
                    messages.add(ChatMessage(UUID.randomUUID().toString(), text, isUser = true))
                    sending = true
                    scope.launch {
                        val api = BridgeApi(AppState.serverUrl, AppState.apiKey)
                        val reply = try {
                            api.chat(modelId(), text)
                        } catch (e: Exception) {
                            "[Bridge Error] ${e.message}"
                        }
                        messages.add(ChatMessage(UUID.randomUUID().toString(), reply, isUser = false))
                        sending = false
                    }
                },
                modifier = Modifier.imePadding(),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (messages.isEmpty()) {
                ChatEmptyPlaceholder(modelId = modelId())
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(msg)
                    }
                }
            }
            if (sending) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
            }
        }
    }
}
