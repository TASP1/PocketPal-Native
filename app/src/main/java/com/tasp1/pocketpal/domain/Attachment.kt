package com.tasp1.pocketpal.domain

import android.net.Uri

data class Attachment(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val mime: String,
    val sizeBytes: Long,
    val kind: Kind,
    /** data:image/...;base64,... for vision API */
    val dataUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val ocrText: String? = null,
    /** UTF-8 text excerpt for text/code previews */
    val textPreview: String? = null,
) {
    val isImage: Boolean get() = kind == Kind.Image
    val sizeLabel: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1_000_000 -> "${sizeBytes / 1024} KB"
            else -> "%.1f MB".format(sizeBytes / 1_000_000.0)
        }

    enum class Kind {
        Image, Pdf, Text, Audio, Video, Spreadsheet, Archive, Code, Other
    }

    companion object {
        const val HARD_LIMIT_BYTES = 50L * 1_000_000
        const val VISION_HIGH_EDGE = 2048
        const val VISION_LOW_EDGE = 1024
        const val JPEG_QUALITY_HIGH = 92
        const val JPEG_QUALITY_BALANCED = 82
        const val TEXT_PREVIEW_CHARS = 12_000
    }
}
