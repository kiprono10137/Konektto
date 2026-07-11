package com.example.konektto.konektto.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.konektto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Two channels, deliberately not one: a message from a friend and a
 * "so-and-so wants to connect" ping are not the same kind of interruption.
 * Letting a person mute friend-request noise without muting messages (or
 * vice versa) is a small thing that makes a real difference in how a chat
 * app feels to live with day to day.
 */
object NotificationHelper {

    const val CHANNEL_MESSAGES = "messages"
    const val CHANNEL_SOCIAL = "social"

    fun createNotificationChannels(context: Context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val messagesChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New private messages from other Konektto users"
        }

        val socialChannel = NotificationChannel(
            CHANNEL_SOCIAL,
            "Friends & Requests",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Friend requests and friend request updates"
        }

        manager.createNotificationChannel(messagesChannel)
        manager.createNotificationChannel(socialChannel)

    }

    /**
     * Fetches the current FCM registration token and stores it on the
     * user's Firestore doc, so the Cloud Function backend knows where to
     * deliver a push. Safe to call repeatedly (e.g. every app launch) --
     * tokens can rotate, and merge() means this never clobbers anything
     * else on the user's document.
     */
    fun refreshFcmToken() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(mapOf("fcmToken" to token), SetOptions.merge())

            }

    }

    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        targetIntent: Intent
    ) {

        targetIntent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            targetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context)
            .notify(notificationId, notification)

    }

}
