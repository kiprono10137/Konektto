package com.example.konektto.konektto.services

import android.content.Intent
import com.example.konektto.konektto.activities.FriendRequestsActivity
import com.example.konektto.konektto.activities.MainActivity
import com.example.konektto.konektto.activities.PrivateChatActivity
import com.example.konektto.konektto.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * The backend (Cloud Functions -- see /functions in the repo root) always
 * sends data-only payloads, deliberately never a top-level "notification"
 * payload. If it sent both, Android would auto-display the notification
 * itself whenever the app isn't in the foreground and skip
 * onMessageReceived entirely -- which means we'd lose the ability to
 * build the correct deep-link intent (open this specific chat, not just
 * the app). Data-only keeps us in control in every app state.
 */
class KonecttoMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        NotificationHelper.refreshFcmToken()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data

        val type = data["type"] ?: return
        val title = data["title"] ?: "Konektto"
        val body = data["body"] ?: ""
        val channelId = data["channelId"] ?: NotificationHelper.CHANNEL_MESSAGES
        val senderId = data["senderId"]

        val targetIntent = when (type) {

            "private_message" ->
                Intent(this, PrivateChatActivity::class.java).apply {
                    putExtra("receiverId", senderId)
                }

            "friend_request", "friend_accepted" ->
                Intent(this, FriendRequestsActivity::class.java)

            else ->
                Intent(this, MainActivity::class.java)

        }

        NotificationHelper.showNotification(
            context = this,
            channelId = channelId,
            notificationId = System.currentTimeMillis().toInt(),
            title = title,
            body = body,
            targetIntent = targetIntent
        )

    }

}
