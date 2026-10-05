package com.tasp1.pocketpal.domain

import android.net.Uri

/**
 * Large-attachment model: images for vision, docs for OCR/context.
 * [maxBytes] default allows ~25 MB before aggressive recompress.
 */
data class Attachment(
    val id: String,
    val uri: Uri,
    val mime: String,
    val displayName: String,
    val sizeBytes: Long,
    val kind: Kind,
    /** Base64 data-URL for vision API (image/* only) */
    val dataUrl: String? = null,
    /** On-device OCR text when available */
    val ocrText: String? = null,
    val width: Int? = null,
    val height: Int? = null,
) {
    enum class Kind { Image, Pdf, Text, Other }

    val isImage: Boolean get() = kind == Kind.Image
    val sizeLabel: String
        get() = when {
            sizeBytes >= 1_000_000 -> "%.1f MB".format(sizeBytes / 1_000_000.0)
            sizeBytes >= 1_000 -> "%.0f KB".format(sizeBytes / 1_000.0)
            else -> "$sizeBytes B"
        }

    companion object {
        /** Soft limit before we recompress images aggressively */
        const val SOFT_LIMIT_BYTES = 12L * 1024 * 1024
        /** Hard reject (user must pick smaller / we refuse) */
        const val HARD_LIMIT_BYTES = 40L * 1024 * 1024
        /** Vision long-edge for "high" detail */
        const val VISION_HIGH_EDGE = 2048
        /** Vision long-edge for "low" / draft */
        const val VISION_LOW_EDGE = 1024
        const val JPEG_QUALITY_HIGH = 92
        const val JPEG_QUALITY_BALANCED = 82
    }
}
