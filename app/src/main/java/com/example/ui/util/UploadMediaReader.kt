package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import kotlin.math.max

sealed interface UploadedDocumentPayload {
    data class PlainText(val fileName: String, val text: String) : UploadedDocumentPayload
    data class BinaryDocument(val fileName: String, val mimeType: String, val bytes: ByteArray) : UploadedDocumentPayload
}

object UploadMediaReader {
    private const val MAX_IMAGE_DIMENSION = 1600
    private const val MAX_DOCUMENT_BYTES = 12 * 1024 * 1024 // 12 MB ceiling safely under Gemini 20 MB inline limit
    private const val MAX_PLAIN_TEXT_CHARS = 12_000

    fun readScaledBitmap(context: Context, uri: Uri): Result<Bitmap> = runCatching {
        val resolver = context.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: error("Unable to open selected image.")

        val largest = max(boundsOptions.outWidth, boundsOptions.outHeight)
        var sampleSize = 1
        while (largest / sampleSize > MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: error("Selected file is not a supported image.")

        // Re-encode if needed or return decoded downsampled bitmap
        bitmap
    }

    fun readDocument(context: Context, uri: Uri): Result<UploadedDocumentPayload> = runCatching {
        val resolver = context.contentResolver
        var fileName = "Uploaded Document"
        var declaredSize = -1L

        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) declaredSize = cursor.getLong(sizeIdx)
                }
            }

        if (declaredSize > MAX_DOCUMENT_BYTES) {
            error("File \"$fileName\" exceeds the 12 MB upload limit.")
        }

        val mimeType = resolver.getType(uri) ?: inferMimeType(fileName)
        val rawBytes = resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(chunk)
                if (read <= 0) break
                total += read
                if (total > MAX_DOCUMENT_BYTES) {
                    error("File \"$fileName\" exceeds the 12 MB upload limit.")
                }
                buffer.write(chunk, 0, read)
            }
            buffer.toByteArray()
        } ?: error("Could not read file \"$fileName\".")

        if (rawBytes.isEmpty()) {
            error("Selected file \"$fileName\" is empty.")
        }

        val isPlainText = mimeType.startsWith("text/") ||
            fileName.endsWith(".txt", ignoreCase = true) ||
            fileName.endsWith(".md", ignoreCase = true) ||
            fileName.endsWith(".csv", ignoreCase = true) ||
            fileName.endsWith(".json", ignoreCase = true)

        if (isPlainText) {
            val content = rawBytes.toString(Charsets.UTF_8).trim()
            if (content.isEmpty()) {
                error("Text file \"$fileName\" contains no readable text.")
            }
            UploadedDocumentPayload.PlainText(
                fileName = fileName,
                text = content.take(MAX_PLAIN_TEXT_CHARS)
            )
        } else {
            val normalizedMime = if (mimeType.isBlank() || mimeType == "application/octet-stream") {
                inferMimeType(fileName)
            } else {
                mimeType
            }
            UploadedDocumentPayload.BinaryDocument(
                fileName = fileName,
                mimeType = normalizedMime,
                bytes = rawBytes
            )
        }
    }

    private fun inferMimeType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".md") -> "text/markdown"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            else -> "text/plain"
        }
    }
}
