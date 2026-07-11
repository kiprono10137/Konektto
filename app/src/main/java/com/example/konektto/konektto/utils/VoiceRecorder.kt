package com.example.konektto.konektto.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

/**
 * A voice note is really just: start recording to a temp file, stop,
 * hand back the file path and how long it ran. Everything above that
 * (uploading, sending, playback) is handled elsewhere -- this class's
 * only job is turning the microphone on and off correctly.
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMillis: Long = 0L

    fun start(): Boolean {

        return try {

            val file = File(
                context.cacheDir,
                "voice_note_${System.currentTimeMillis()}.m4a"
            )

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            outputFile = file
            startTimeMillis = System.currentTimeMillis()

            true

        } catch (e: IOException) {

            release()
            false

        } catch (e: IllegalStateException) {

            release()
            false

        }

    }

    /**
     * Returns the recorded file path and duration, or null if there was
     * nothing usable to stop (e.g. start() failed and this got called
     * anyway).
     */
    fun stop(): Pair<String, Long>? {

        val file = outputFile ?: return null
        val duration = System.currentTimeMillis() - startTimeMillis

        return try {

            recorder?.apply {
                stop()
                release()
            }

            recorder = null
            outputFile = null

            file.absolutePath to duration

        } catch (e: Exception) {

            release()
            null

        }

    }

    /** Discards the in-progress recording without producing a message. */
    fun cancel() {

        release()
        outputFile?.delete()
        outputFile = null

    }

    private fun release() {

        try {
            recorder?.release()
        } catch (e: Exception) {
            // already released or never started -- nothing to clean up
        }

        recorder = null

    }

}
