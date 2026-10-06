package com.tasp1.pocketpal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tasp1.pocketpal.ui.components.claude.ClaudeHomeEmpty

@Composable
fun ChatEmptyPlaceholder(
    modelId: String,
    userName: String = "Adarsh",
    modifier: Modifier = Modifier,
) {
    ClaudeHomeEmpty(userName = userName, modifier = modifier)
}
