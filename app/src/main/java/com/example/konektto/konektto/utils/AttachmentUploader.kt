package com.example.konektto.konektto.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/**
 * One upload pipeline for every kind of attachment Konektto sends --
 * photo, GIF, voice note, or generic file. All four are really the same
 * problem (turn a Uri or local file into bytes, upload to Cloudinary,
 * hand back a URL) with only the encoding step differing. Splitting this
 * into four unrelated feature implementations would have meant four
 * places to fix the same bug later.
 *
 * All callbacks land on the main thread, guaranteed -- callers never need
 * to remember to runOnUiThread themselves, because the one thing every
 * caller here will do with the result is touch a View.
 */
object AttachmentUploader {

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun postToMain(action: () -> Unit) {
        mainHandler.post(action)
    }

    /**
     * Static photos are resized (max 1600px on the longest side) and
     * recompressed to JPEG @ 85% -- nobody wants a chat costing someone
     * their whole data plan because their phone camera shoots 12MB
     * originals. Animated GIFs are the one exception: they're uploaded as
     * raw bytes, completely untouched, because decoding through Bitmap
     * would keep only the first frame and throw away the animation.
     */
    fun uploadImageOrGif(
        context: Context,
        uri: Uri,
        callback: (url: String?) -> Unit
    ) {

        Thread {

            try {

                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

                if (mimeType == "image/gif") {

                    val bytes = context.contentResolver
                        .openInputStream(uri)?.use { it.readBytes() }

                    if (bytes == null) {
                        postToMain { callback(null) }
                        return@Thread
                    }

                    CloudinaryUploader.uploadFile(
                        bytes = bytes,
                        filename = "attachment.gif",
                        mimeType = "image/gif",
                        resourceType = "image"
                    ) { url -> postToMain { callback(url) } }

                } else {

                    val bitmap = decodeBitmap(context, uri)

                    if (bitmap == null) {
                        postToMain { callback(null) }
                        return@Thread
                    }

                    val resized = scaleDownIfNeeded(bitmap, 1600)

                    val outputStream = ByteArrayOutputStream()
                    resized.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

                    CloudinaryUploader.uploadFile(
                        bytes = outputStream.toByteArray(),
                        filename = "attachment.jpg",
                        mimeType = "image/jpeg",
                        resourceType = "image"
                    ) { url -> postToMain { callback(url) } }

                }

            } catch (e: Exception) {

                postToMain { callback(null) }

            }

        }.start()

    }

    /**
     * Cloudinary has no dedicated "audio" resource type -- audio-only
     * files are uploaded under "video", which is Cloudinary's own
     * convention, not a mistake here.
     */
    fun uploadAudio(
        filePath: String,
        callback: (url: String?) -> Unit
    ) {

        Thread {

            try {

                val bytes = File(filePath).readBytes()

                CloudinaryUploader.uploadFile(
                    bytes = bytes,
                    filename = "voice_note.m4a",
                    mimeType = "audio/mp4",
                    resourceType = "video"
                ) { url -> postToMain { callback(url) } }

            } catch (e: Exception) {

                postToMain { callback(null) }

            }

        }.start()

    }

    /**
     * Returns the uploaded URL alongside the file's original display name
     * and size -- both needed to render a sensible file card in chat.
     * A message bubble that just shows a bare Cloudinary URL tells the
     * recipient nothing about what they're about to open.
     */
    fun uploadGenericFile(
        context: Context,
        uri: Uri,
        callback: (url: String?, filename: String, sizeBytes: Long) -> Unit
    ) {

        Thread {

            val (filename, sizeBytes) = queryFileMeta(context, uri)

            try {

                val mimeType =
                    context.contentResolver.getType(uri) ?: "application/octet-stream"

                val bytes = context.contentResolver
                    .openInputStream(uri)?.use { it.readBytes() }

                if (bytes == null) {
                    postToMain { callback(null, filename, sizeBytes) }
                    return@Thread
                }

                CloudinaryUploader.uploadFile(
                    bytes = bytes,
                    filename = filename,
                    mimeType = mimeType,
                    resourceType = "raw"
                ) { url -> postToMain { callback(url, filename, sizeBytes) } }

            } catch (e: Exception) {

                postToMain { callback(null, filename, sizeBytes) }

            }

        }.start()

    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)

        } else {

            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

        }

    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {

        val largestSide = max(bitmap.width, bitmap.height)

        if (largestSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / largestSide

        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )

    }

    private fun queryFileMeta(context: Context, uri: Uri): Pair<String, Long> {

        var name = "file"
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->

            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

            if (cursor.moveToFirst()) {

                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)

            }

        }

        return name to size

    }

    /** "482 KB", "3.1 MB" -- for displaying a file attachment's size in chat. */
    fun formatFileSize(bytes: Long): String {

        if (bytes <= 0) return "0 KB"

        val kb = bytes / 1024.0
        if (kb < 1024) return "${kb.toInt()} KB"

        val mb = kb / 1024.0
        return String.format("%.1f MB", mb)

    }

}
