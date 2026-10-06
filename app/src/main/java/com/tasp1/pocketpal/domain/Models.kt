package com.tasp1.pocketpal.domain

data class ChatTurn(
    val id: String,
    val role: Role,
    val content: String,
    val reasoning: String = "",
    val sourcesJson: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
    /** Image data-URLs or local URIs shown in the bubble */
    val imageDataUrls: List<String> = emptyList(),
    val attachmentNames: List<String> = emptyList(),
    val ocrPreview: String? = null,
    val toolSteps: List<ToolStep> = emptyList(),
) {
    enum class Role { User, Assistant }
}

data class RemoteModel(
    val id: String,
    val ownedBy: String = "kaggle-bridge",
)

data class HealthStatus(
    val ok: Boolean,
    val version: String?,
    val backendConnected: Boolean,
    val caps: List<String>,
)

data class LocalModel(
    val id: String,
    val name: String,
    val sizeLabel: String,
    val type: String,
    val status: Status,
) {
    enum class Status { Available, Downloading, Ready, Unavailable }
}
