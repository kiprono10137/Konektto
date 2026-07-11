package com.example.konektto.konektto.utils

import android.media.MediaPlayer

/**
 * Without this, tapping play on a second voice note while a first is
 * still playing would overlap both into noise. Starting any playback
 * here always stops whatever was already playing first.
 */
object AudioPlaybackManager {

    private var currentPlayer: MediaPlayer? = null
    private var currentUrl: String? = null
    private var onCurrentStopped: (() -> Unit)? = null

    fun isPlaying(url: String): Boolean {
        return currentUrl == url && currentPlayer?.isPlaying == true
    }

    fun play(url: String, onCompletion: () -> Unit, onStopped: () -> Unit) {

        stop()

        val player = MediaPlayer()

        try {

            player.setDataSource(url)
            player.setOnPreparedListener { it.start() }
            player.setOnCompletionListener {
                onCompletion()
                release()
            }

            player.prepareAsync()

            currentPlayer = player
            currentUrl = url
            onCurrentStopped = onStopped

        } catch (e: Exception) {

            release()

        }

    }

    fun stop() {

        onCurrentStopped?.invoke()
        release()

    }

    private fun release() {

        try {
            currentPlayer?.release()
        } catch (e: Exception) {
            // already released -- nothing to clean up
        }

        currentPlayer = null
        currentUrl = null
        onCurrentStopped = null

    }

}
