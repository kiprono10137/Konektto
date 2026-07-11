package com.example.konektto.konektto.utils

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.konektto.R

/**
 * Owns the whole record -> live timer -> stop/cancel flow behind one call.
 * Both chat screens call this identically; neither needs to know anything
 * about MediaRecorder itself.
 */
object VoiceRecorderDialog {

    fun show(
        context: Context,
        onRecorded: (filePath: String, durationMs: Long) -> Unit
    ) {

        val recorder = VoiceRecorder(context)

        if (!recorder.start()) {
            Toast.makeText(context, "Couldn't start recording.", Toast.LENGTH_SHORT).show()
            return
        }

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_voice_recorder, null)

        val tvTimer = view.findViewById<TextView>(R.id.tvRecordingTimer)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelRecording)
        val btnStop = view.findViewById<Button>(R.id.btnStopRecording)

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        val handler = Handler(Looper.getMainLooper())
        val startTime = System.currentTimeMillis()

        val tickRunnable = object : Runnable {
            override fun run() {
                tvTimer.text = TimeUtils.formatDuration(System.currentTimeMillis() - startTime)
                handler.postDelayed(this, 200L)
            }
        }

        handler.post(tickRunnable)

        btnCancel.setOnClickListener {

            handler.removeCallbacks(tickRunnable)
            recorder.cancel()
            dialog.dismiss()

        }

        btnStop.setOnClickListener {

            handler.removeCallbacks(tickRunnable)
            val result = recorder.stop()
            dialog.dismiss()

            when {

                result == null ->
                    Toast.makeText(context, "Recording failed.", Toast.LENGTH_SHORT).show()

                result.second < 500L ->
                    Toast.makeText(context, "Recording too short.", Toast.LENGTH_SHORT).show()

                else ->
                    onRecorded(result.first, result.second)

            }

        }

        dialog.show()

    }

}
