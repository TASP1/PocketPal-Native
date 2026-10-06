package com.tasp1.pocketpal.domain

/** Claude-style intermediate tool / thinking step shown above the answer. */
data class ToolStep(
    val id: String,
    val title: String,
    val detail: String = "",
    val kind: Kind = Kind.Thinking,
    val request: String? = null,
    val response: String? = null,
    val done: Boolean = true,
) {
    enum class Kind { Thinking, Shell, Search, Read, Bot, Other }
}
