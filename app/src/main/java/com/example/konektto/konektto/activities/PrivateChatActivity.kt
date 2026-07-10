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
import com.example.konektto.konektto.adapters.PrivateMessageAdapter
import com.example.konektto.konektto.models.PrivateMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PrivateChatActivity : AppCompatActivity() {

    private lateinit var tvReceiver: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    private lateinit var adapter: PrivateMessageAdapter
    private lateinit var db: FirebaseFirestore

    private val messageList = mutableListOf<PrivateMessage>()

    private lateinit var currentUserId: String
    private lateinit var receiverId: String
    private lateinit var chatId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_chat)

        supportActionBar?.hide()

        tvReceiver = findViewById(R.id.tvReceiver)
        rvMessages = findViewById(R.id.rvPrivateMessages)
        etMessage = findViewById(R.id.etPrivateMessage)
        btnSend = findViewById(R.id.btnSendPrivate)

        db = FirebaseFirestore.getInstance()

        currentUserId =
            FirebaseAuth.getInstance().currentUser?.uid ?: return

        receiverId =
            intent.getStringExtra("receiverId") ?: return

        chatId = generateChatId(
            currentUserId,
            receiverId
        )

        adapter = PrivateMessageAdapter(messageList)

        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        loadReceiverName()
        listenForMessages()

        btnSend.setOnClickListener {

            sendMessage()

        }

    }
    private fun generateChatId(
        user1: String,
        user2: String
    ): String {

        return if (user1 < user2) {
            "${user1}_${user2}"
        } else {
            "${user2}_${user1}"
        }

    }

    private fun loadReceiverName() {

        db.collection("users")
            .document(receiverId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val username =
                        document.getString("username") ?: "User"

                    tvReceiver.text = username

                } else {

                    tvReceiver.text = "Private Chat"

                }

            }
            .addOnFailureListener {

                tvReceiver.text = "Private Chat"

            }

    }

    private fun listenForMessages() {

        db.collection("privateChats")
            .document(chatId)
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

                snapshots?.documents?.forEach { document ->

                    val message =
                        document.toObject(PrivateMessage::class.java)

                    if (message != null) {

                        messageList.add(message)

                    }

                }

                adapter.notifyDataSetChanged()

                if (messageList.isNotEmpty()) {

                    rvMessages.scrollToPosition(
                        messageList.size - 1
                    )

                }

            }

    }
    private fun sendMessage() {

        val text = etMessage.text.toString().trim()

        if (text.isEmpty()) {

            etMessage.error = "Enter a message"
            etMessage.requestFocus()
            return

        }

        val messageRef = db.collection("privateChats")
            .document(chatId)
            .collection("messages")
            .document()

        val message = PrivateMessage(
            messageId = messageRef.id,
            senderId = currentUserId,
            receiverId = receiverId,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        messageRef.set(message)
            .addOnSuccessListener {

                val chatData = hashMapOf<String, Any>(
                    "participants" to listOf(currentUserId, receiverId),
                    "lastMessage" to text,
                    "lastTimestamp" to System.currentTimeMillis()
                )

                db.collection("privateChats")
                    .document(chatId)
                    .set(chatData)

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

}