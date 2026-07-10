package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.R
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var tvUsername: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvRoomsJoined: TextView
    private lateinit var tvMessagesSent: TextView
    private lateinit var btnMessage: Button

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        supportActionBar?.hide()

        db = FirebaseFirestore.getInstance()

        tvUsername = findViewById(R.id.tvUsername)
        tvBio = findViewById(R.id.tvBio)
        tvRoomsJoined = findViewById(R.id.tvRoomsJoined)
        tvMessagesSent = findViewById(R.id.tvMessagesSent)
        btnMessage = findViewById(R.id.btnMessage)

        userId = intent.getStringExtra("userId") ?: return

        loadProfile(userId)

        btnMessage.setOnClickListener {

            val intent = Intent(
                this,
                PrivateChatActivity::class.java
            )

            intent.putExtra("receiverId", userId)

            startActivity(intent)

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

}