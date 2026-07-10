package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
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
    private lateinit var tvCreator: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvMembers: TextView

    private lateinit var tvAnnouncement: TextView

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText

    private lateinit var btnSend: Button
    private lateinit var btnViewMembers: Button
    private lateinit var btnLeaveRoom: Button
    private lateinit var btnAdminPanel: Button

    private lateinit var adapter: MessageAdapter
    private lateinit var db: FirebaseFirestore

    private val messageList = mutableListOf<Message>()

    private lateinit var roomId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_dashboard)

        supportActionBar?.hide()

        tvRoomTitle = findViewById(R.id.tvRoomTitle)
        tvCreator = findViewById(R.id.tvCreator)
        tvCategory = findViewById(R.id.tvCategory)
        tvMembers = findViewById(R.id.tvMembers)
        tvAnnouncement = findViewById(R.id.tvAnnouncement)


        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)

        btnSend = findViewById(R.id.btnSend)
        btnViewMembers = findViewById(R.id.btnViewMembers)
        btnLeaveRoom = findViewById(R.id.btnLeaveRoom)
        btnAdminPanel = findViewById(R.id.btnAdminPanel)

        db = FirebaseFirestore.getInstance()

        roomId = intent.getStringExtra("roomId") ?: ""

        adapter = MessageAdapter(messageList) { message ->
            deleteMessage(message)
        }

        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        loadRoomDetails()
        listenForMessages()

        btnSend.setOnClickListener {
            sendMessage()
        }

        btnViewMembers.setOnClickListener {

            val intent = Intent(
                this,
                MembersActivity::class.java
            )

            intent.putExtra("roomId", roomId)

            startActivity(intent)

        }

        btnLeaveRoom.setOnClickListener {
            leaveRoom()
        }

        btnAdminPanel.setOnClickListener {

            val intent = Intent(
                this,
                CommunityAdminActivity::class.java
            )

            intent.putExtra("roomId", roomId)

            startActivity(intent)

        }

    }
    private fun loadRoomDetails() {

        db.collection("rooms")
            .document(roomId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) return@addOnSuccessListener

                tvRoomTitle.text =
                    document.getString("roomName")

                tvCreator.text =
                    "Created by: ${document.getString("creatorUsername")}"

                val category =
                    document.getString("category") ?: "General"

                tvCategory.text =
                    "Category: $category"

                val members =
                    document.getLong("memberCount") ?: 1

                tvMembers.text =
                    "👥 $members Members"

                // Show Admin Panel only to the room creator
                val creatorId =
                    document.getString("creatorId")

                val currentUser =
                    FirebaseAuth.getInstance().currentUser

                if (currentUser != null &&
                    creatorId == currentUser.uid
                ) {

                    btnAdminPanel.visibility = View.VISIBLE

                } else {

                    btnAdminPanel.visibility = View.GONE

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

                    val message =
                        document.toObject(Message::class.java)

                    messageList.add(message)

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

                val username =
                    document.getString("username") ?: "Anonymous"

                val messageRef = db.collection("rooms")
                    .document(roomId)
                    .collection("messages")
                    .document()

                val message = Message(
                    messageId = messageRef.id,
                    senderId = currentUser.uid,
                    senderName = username,
                    text = text,
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

    private fun deleteMessage(message: Message) {

        db.collection("rooms")
            .document(roomId)
            .collection("messages")
            .document(message.messageId)
            .delete()
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Message deleted.",
                    Toast.LENGTH_SHORT
                ).show()

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()

            }

    }

    private fun leaveRoom() {

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val roomRef = db.collection("rooms").document(roomId)

        roomRef.collection("members")
            .document(currentUser.uid)
            .delete()
            .addOnSuccessListener {

                roomRef.get()
                    .addOnSuccessListener { document ->

                        val currentCount =
                            document.getLong("memberCount") ?: 1

                        val newCount =
                            if (currentCount > 0)
                                currentCount - 1
                            else
                                0

                        roomRef.update(
                            "memberCount",
                            newCount
                        )

                        Toast.makeText(
                            this,
                            "You left the community.",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()

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
    private fun loadAnnouncement() {

        db.collection("rooms")
            .document(roomId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {

                    val announcement =
                        snapshot.getString("announcement") ?: ""

                    if (announcement.isNotEmpty()) {

                        tvAnnouncement.visibility = View.VISIBLE

                        tvAnnouncement.text =
                            "📢 Announcement\n\n$announcement"

                    } else {

                        tvAnnouncement.visibility = View.GONE

                    }

                }

            }

    }

}