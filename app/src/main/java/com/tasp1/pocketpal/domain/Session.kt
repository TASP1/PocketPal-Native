package com.tasp1.pocketpal.domain

import kotlinx.serialization.Serializable

@Serializable
data class ChatSession(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val modelId: String = "",
    val preview: String = "",
)

@Serializable
data class StoredTurn(
    val id: String,
    val role: String, // user | assistant
    val content: String,
    val reasoning: String = "",
    val sourcesJson: String = "",
)

@Serializable
data class SessionPayload(
    val session: ChatSession,
    val turns: List<StoredTurn> = emptyList(),
)
