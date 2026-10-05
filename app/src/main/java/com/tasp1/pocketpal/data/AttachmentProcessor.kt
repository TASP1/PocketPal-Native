package com.tasp1.pocketpal.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tasp1.pocketpal.domain.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

class AttachmentProcessor(private val context: Context) {

    enum class VisionQuality { High, Balanced, Low }

    suspend fun fromUri(
        uri: Uri,
        quality: VisionQuality = VisionQuality.High,
        runOcr: Boolean = true,
    ): Attachment = withContext(Dispatchers.IO) {
        val cr = context.contentResolver
        val mime = cr.getType(uri) ?: "application/octet-stream"
        val (name, size) = queryNameSize(uri)
        if (size > Attachment.HARD_LIMIT_BYTES) {
            error("File too large (${size / 1_000_000} MB). Max ${Attachment.HARD_LIMIT_BYTES / 1_000_000} MB.")
        }
        val kind = when {
            mime.startsWith("image/") -> Attachment.Kind.Image
            mime == "application/pdf" -> Attachment.Kind.Pdf
            mime.startsWith("text/") -> Attachment.Kind.Text
            else -> Attachment.Kind.Other
        }

        var dataUrl: String? = null
        var width: Int? = null
        var height: Int? = null
        var ocr: String? = null

        if (kind == Attachment.Kind.Image) {
            val bmp = decodeBitmap(uri)
            val edge = when (quality) {
                VisionQuality.High -> Attachment.VISION_HIGH_EDGE
                VisionQuality.Balanced -> 1536
                VisionQuality.Low -> Attachment.VISION_LOW_EDGE
            }
            val q = when (quality) {
                VisionQuality.High -> Attachment.JPEG_QUALITY_HIGH
                VisionQuality.Balanced -> Attachment.JPEG_QUALITY_BALANCED
                VisionQuality.Low -> 72
            }
            val scaled = scaleLongEdge(bmp, edge)
            width = scaled.width
            height = scaled.height
            val jpeg = toJpeg(scaled, q)
            // Free large originals
            if (scaled !== bmp) bmp.recycle()
            dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP)
            if (runOcr) {
                ocr = runCatching { recognizeText(uri, scaled) }.getOrNull()
            }
            if (scaled !== bmp) {
                // already recycled bmp if different; scaled still needed until OCR done
            }
            scaled.recycle()
        } else if (kind == Attachment.Kind.Text || mime == "application/json") {
            cr.openInputStream(uri)?.use { ins ->
                ocr = ins.readBytes().toString(Charsets.UTF_8).take(200_000)
            }
        }

        Attachment(
            id = UUID.randomUUID().toString(),
            uri = uri,
            mime = mime,
            displayName = name,
            sizeBytes = size,
            kind = kind,
            dataUrl = dataUrl,
            ocrText = ocr,
            width = width,
            height = height,
        )
    }

    private fun queryNameSize(uri: Uri): Pair<String, Long> {
        var name = "attachment"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val si = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst()) {
                if (ni >= 0) name = c.getString(ni) ?: name
                if (si >= 0) size = c.getLong(si)
            }
        }
        if (size <= 0) {
            context.contentResolver.openInputStream(uri)?.use { size = it.available().toLong() }
        }
        return name to size
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= 28) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                decoder.isMutableRequired = false
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it) ?: error("Cannot decode image")
            }
        }
    }

    private fun scaleLongEdge(src: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = max(src.width, src.height)
        if (longEdge <= maxEdge) return src
        val scale = maxEdge.toFloat() / longEdge
        val w = (src.width * scale).roundToInt().coerceAtLeast(1)
        val h = (src.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun toJpeg(bmp: Bitmap, quality: Int): ByteArray {
        val os = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, os)
        return os.toByteArray()
    }

    private suspend fun recognizeText(uri: Uri, bmp: Bitmap?): String =
        suspendCancellableCoroutine { cont ->
            val image = try {
                if (bmp != null) InputImage.fromBitmap(bmp, 0)
                else InputImage.fromFilePath(context, uri)
            } catch (e: Exception) {
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            }
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    cont.resume(result.text.trim())
                    recognizer.close()
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                    recognizer.close()
                }
        }
}
