package com.tasp1.pocketpal.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tasp1.pocketpal.data.BridgeApi
import com.tasp1.pocketpal.data.BridgeConfig
import com.tasp1.pocketpal.ui.components.ChatBubble
import com.tasp1.pocketpal.ui.components.ChatInputBar
import com.tasp1.pocketpal.ui.components.ChatMessage
import com.tasp1.pocketpal.ui.components.ChatTopBar
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ChatScreen(
    onOpenDrawer: () -> Unit,
) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var think by remember { mutableStateOf(false) }
    var modelId by remember { mutableStateOf(BridgeConfig.DEFAULT_MODEL + ":web") }
    var title by remember { mutableStateOf("Hi") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val api = remember { BridgeApi() }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun effectiveModel(): String {
        var m = modelId.substringBefore(":web").substringBefore(":think").substringBefore(":shell")
            .ifBlank { BridgeConfig.DEFAULT_MODEL }
        // rebuild flags from UI toggles (Think chip + keep :web for search demo parity)
        val parts = mutableListOf(m)
        if (modelId.contains(":web")) parts.add("web")
        if (think) parts.add("think")
        return parts.joinToString(":")
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                title = title,
                modelId = modelId,
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
                thinkEnabled = think,
                onToggleThink = { think = !think },
                onSend = {
                    val text = input.trim()
                    if (text.isEmpty() || sending) return@ChatInputBar
                    input = ""
                    if (title == "New chat" || title == "Hi") title = text.take(40)
                    messages.add(ChatMessage(UUID.randomUUID().toString(), text, isUser = true))
                    sending = true
                    scope.launch {
                        val reply = try {
                            api.chat(effectiveModel(), text)
                        } catch (e: Exception) {
                            "[Bridge Error] ${e.message}"
                        }
                        messages.add(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                text = reply,
                                isUser = false,
                            ),
                        )
                        sending = false
                    }
                },
                modifier = Modifier.imePadding(),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg)
            }
        }
    }
}
