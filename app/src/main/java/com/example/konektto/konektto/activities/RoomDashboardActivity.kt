package com.example.konektto.konektto.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.adapters.MessageAdapter
import com.example.konektto.konektto.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RoomDashboardActivity : AppCompatActivity() {

    private lateinit var tvRoomTitle: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    private lateinit var adapter: MessageAdapter
    private lateinit var db: FirebaseFirestore

    private val messageList = mutableListOf<Message>()

    private lateinit var roomId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_dashboard)

        tvRoomTitle = findViewById(R.id.tvRoomTitle)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        db = FirebaseFirestore.getInstance()

        roomId = intent.getStringExtra("roomId") ?: ""
        val roomName = intent.getStringExtra("roomName") ?: "Room"

        tvRoomTitle.text = roomName

        adapter = MessageAdapter(messageList)

        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        listenForMessages()

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun listenForMessages() {

        db.collection("rooms")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->

                if (error != null) {
                    Toast.makeText(
                        this,
                        error.localizedMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                messageList.clear()

                snapshots?.forEach { document ->
                    val message = document.toObject(Message::class.java)
                    messageList.add(message)
                }

                adapter.notifyDataSetChanged()

                if (messageList.isNotEmpty()) {
                    rvMessages.scrollToPosition(messageList.size - 1)
                }
            }
    }

    private fun sendMessage() {

        val text = etMessage.text.toString().trim()

        if (text.isEmpty()) {
            etMessage.error = "Enter a message"
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            Toast.makeText(
                this,
                "Please log in again.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                val username = document.getString("username") ?: "Anonymous"

                val messageRef = db.collection("rooms")
                    .document(roomId)
                    .collection("messages")
                    .document()

                val message = Message(
                    messageId = messageRef.id,
                    roomId = roomId,
                    senderId = currentUser.uid,
                    senderName = username,
                    message = text,
                    timestamp = System.currentTimeMillis()
                )

                messageRef.set(message)
                    .addOnSuccessListener {
                        etMessage.text.clear()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            e.localizedMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }

            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}