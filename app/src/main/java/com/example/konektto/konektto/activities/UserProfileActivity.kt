package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.konektto.R
import com.example.konektto.konektto.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UserProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvUsername: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvRoomsJoined: TextView
    private lateinit var tvMessagesSent: TextView
    private lateinit var btnMessage: Button
    private lateinit var btnFriend: Button

    private lateinit var userId: String
    private var currentUserId: String? = null
    private var presenceListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        supportActionBar?.hide()

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid

        tvUsername = findViewById(R.id.tvUsername)
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus)
        tvBio = findViewById(R.id.tvBio)
        tvRoomsJoined = findViewById(R.id.tvRoomsJoined)
        tvMessagesSent = findViewById(R.id.tvMessagesSent)
        btnMessage = findViewById(R.id.btnMessage)
        btnFriend = findViewById(R.id.btnFriend)

        userId = intent.getStringExtra("userId") ?: return

        loadProfile(userId)

        if (userId == currentUserId) {

            // Viewing your own profile (e.g. from a member list) --
            // messaging/friending yourself doesn't make sense.
            btnMessage.visibility = View.GONE
            btnFriend.visibility = View.GONE

        } else {

            btnMessage.setOnClickListener {

                val intent = Intent(
                    this,
                    PrivateChatActivity::class.java
                )

                intent.putExtra("receiverId", userId)

                startActivity(intent)

            }

            refreshFriendButton()
            listenForPresence()

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        presenceListener?.remove()
    }

    private fun listenForPresence() {

        presenceListener = db.collection("users")
            .document(userId)
            .addSnapshotListener { document, error ->

                if (error != null || document == null || !document.exists()) return@addSnapshotListener

                val isOnline = document.getBoolean("isOnline") ?: false
                val lastSeen = document.getTimestamp("lastSeen")?.toDate()?.time ?: 0L

                tvOnlineStatus.visibility = View.VISIBLE

                if (isOnline) {

                    tvOnlineStatus.text = "🟢 Online"
                    tvOnlineStatus.setTextColor(
                        ContextCompat.getColor(this, R.color.konektto_status_online)
                    )

                } else {

                    tvOnlineStatus.text = "Last seen ${TimeUtils.formatLastSeen(lastSeen)}"
                    tvOnlineStatus.setTextColor(
                        com.google.android.material.color.MaterialColors.getColor(
                            tvOnlineStatus,
                            com.google.android.material.R.attr.colorOnSurfaceVariant
                        )
                    )

                }

            }

    }

    private fun loadProfile(userId: String) {

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) return@addOnSuccessListener

                val username =
                    document.getString("username") ?: "Unknown"

                val bio =
                    document.getString("bio") ?: "No bio yet."

                tvUsername.text = username
                tvBio.text = bio

                loadRoomsJoined(userId)
                loadMessagesSent(userId)

            }

    }

    private fun loadRoomsJoined(userId: String) {

        db.collection("rooms")
            .whereEqualTo("creatorId", userId)
            .get()
            .addOnSuccessListener {

                tvRoomsJoined.text =
                    "Rooms Created: ${it.size()}"

            }

    }

    private fun loadMessagesSent(userId: String) {

        db.collectionGroup("messages")
            .whereEqualTo("senderId", userId)
            .get()
            .addOnSuccessListener {

                tvMessagesSent.text =
                    "Messages Sent: ${it.size()}"

            }

    }

    private fun generateRequestId(user1: String, user2: String): String {

        return if (user1 < user2) {
            "${user1}_${user2}"
        } else {
            "${user2}_${user1}"
        }

    }

    private fun refreshFriendButton() {

        val myId = currentUserId ?: return
        val requestId = generateRequestId(myId, userId)

        db.collection("friendRequests")
            .document(requestId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    showAddFriendState(requestId)
                    return@addOnSuccessListener

                }

                val status = document.getString("status") ?: ""
                val senderId = document.getString("senderId") ?: ""

                when {

                    status == "accepted" -> showFriendsState(requestId)

                    status == "pending" && senderId == myId ->
                        showRequestSentState()

                    status == "pending" && senderId == userId ->
                        showAcceptState(requestId)

                    else -> showAddFriendState(requestId)

                }

            }

    }

    private fun showAddFriendState(requestId: String) {

        btnFriend.isEnabled = true
        btnFriend.text = "➕ Add Friend"

        btnFriend.setOnClickListener {

            val myId = currentUserId ?: return@setOnClickListener

            val request = hashMapOf(
                "senderId" to myId,
                "receiverId" to userId,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("friendRequests")
                .document(requestId)
                .set(request)
                .addOnSuccessListener {
                    showRequestSentState()
                }

        }

    }

    private fun showRequestSentState() {

        btnFriend.isEnabled = false
        btnFriend.text = "⏳ Request Sent"
        btnFriend.setOnClickListener(null)

    }

    private fun showAcceptState(requestId: String) {

        btnFriend.isEnabled = true
        btnFriend.text = "✅ Accept Friend Request"

        btnFriend.setOnClickListener {

            db.collection("friendRequests")
                .document(requestId)
                .update("status", "accepted")
                .addOnSuccessListener {
                    showFriendsState(requestId)
                }

        }

    }

    private fun showFriendsState(requestId: String) {

        btnFriend.isEnabled = true
        btnFriend.text = "🤝 Friends (tap to remove)"

        btnFriend.setOnClickListener {

            db.collection("friendRequests")
                .document(requestId)
                .delete()
                .addOnSuccessListener {
                    showAddFriendState(requestId)
                }

        }

    }

}
