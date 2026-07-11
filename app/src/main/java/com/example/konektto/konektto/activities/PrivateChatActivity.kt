package com.example.konektto.konektto.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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
import com.example.konektto.konektto.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class PrivateChatActivity : AppCompatActivity() {

    private lateinit var tvReceiver: TextView
    private lateinit var tvReceiverStatus: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    private lateinit var adapter: PrivateMessageAdapter
    private lateinit var db: FirebaseFirestore

    private val messageList = mutableListOf<PrivateMessage>()

    private lateinit var currentUserId: String
    private lateinit var receiverId: String
    private lateinit var chatId: String

    private var presenceListener: ListenerRegistration? = null
    private var typingListener: ListenerRegistration? = null

    // Presence + typing are two independent streams of truth about the
    // same person, but only one line of UI to show them in. Typing always
    // wins while it's happening; otherwise we fall back to online/last seen.
    private var receiverIsOnline = false
    private var receiverLastSeen = 0L
    private var receiverIsTyping = false

    private val typingHandler = Handler(Looper.getMainLooper())
    private var isCurrentlyTyping = false

    private val stopTypingRunnable = Runnable {
        setTypingStatus(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_chat)

        supportActionBar?.hide()

        tvReceiver = findViewById(R.id.tvReceiver)
        tvReceiverStatus = findViewById(R.id.tvReceiverStatus)
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

        adapter = PrivateMessageAdapter(messageList, currentUserId)

        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        loadReceiverName()
        listenForMessages()
        listenForPresence()
        listenForTyping()

        etMessage.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (s.isNullOrEmpty()) {

                    typingHandler.removeCallbacks(stopTypingRunnable)
                    setTypingStatus(false)

                } else {

                    setTypingStatus(true)

                    // Reset the "stopped typing" timeout on every keystroke.
                    // 2 seconds of silence reads as "done typing" to a human;
                    // no need to write to Firestore on every single character,
                    // just on the true/false transitions.
                    typingHandler.removeCallbacks(stopTypingRunnable)
                    typingHandler.postDelayed(stopTypingRunnable, 2000L)

                }

            }

            override fun afterTextChanged(s: Editable?) {}

        })

        btnSend.setOnClickListener {

            sendMessage()

        }

    }

    override fun onDestroy() {
        super.onDestroy()

        presenceListener?.remove()
        typingListener?.remove()

        typingHandler.removeCallbacks(stopTypingRunnable)
        setTypingStatus(false)

    }

    private fun listenForPresence() {

        presenceListener = db.collection("users")
            .document(receiverId)
            .addSnapshotListener { document, error ->

                if (error != null || document == null || !document.exists()) return@addSnapshotListener

                receiverIsOnline = document.getBoolean("isOnline") ?: false
                receiverLastSeen = document.getTimestamp("lastSeen")?.toDate()?.time ?: 0L

                updateStatusDisplay()

            }

    }

    private fun listenForTyping() {

        typingListener = db.collection("privateChats")
            .document(chatId)
            .addSnapshotListener { document, error ->

                if (error != null || document == null || !document.exists()) return@addSnapshotListener

                @Suppress("UNCHECKED_CAST")
                val typingUsers = document.get("typingUsers") as? Map<String, Boolean>

                receiverIsTyping = typingUsers?.get(receiverId) == true

                updateStatusDisplay()

            }

    }

    private fun updateStatusDisplay() {

        tvReceiverStatus.visibility = View.VISIBLE

        tvReceiverStatus.text = when {

            receiverIsTyping -> "✍️ typing..."

            receiverIsOnline -> "🟢 Online"

            else -> "Last seen ${TimeUtils.formatLastSeen(receiverLastSeen)}"

        }

    }

    private fun setTypingStatus(isTyping: Boolean) {

        if (isCurrentlyTyping == isTyping) return
        isCurrentlyTyping = isTyping

        // Dot-path key on a merge-set creates/updates just this one nested
        // field, without requiring the chat doc to already exist (unlike
        // update(), which fails on a doc that hasn't been created yet --
        // relevant here since two people can open a fresh chat and start
        // typing before either has sent a first message).
        db.collection("privateChats")
            .document(chatId)
            .set(
                mapOf("typingUsers.$currentUserId" to isTyping),
                SetOptions.merge()
            )

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

                markIncomingMessagesAsRead(snapshots?.documents)

            }

    }

    private fun markIncomingMessagesAsRead(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>?
    ) {

        if (documents == null) return

        val batch = db.batch()
        var hasUnread = false

        for (document in documents) {

            val senderId = document.getString("senderId") ?: continue
            val isRead = document.getBoolean("read") ?: false

            // Only mark messages the *other* person sent, that I haven't
            // already seen -- I don't need a receipt on my own messages,
            // and there's no point rewriting fields already at the value
            // I'm about to set them to.
            if (senderId == receiverId && !isRead) {

                batch.update(document.reference, "read", true)
                hasUnread = true

            }

        }

        if (hasUnread) {
            batch.commit()
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
            timestamp = System.currentTimeMillis(),
            read = false
        )

        messageRef.set(message)
            .addOnSuccessListener {

                val chatData = hashMapOf<String, Any>(
                    "participants" to listOf(currentUserId, receiverId),
                    "lastMessage" to text,
                    "lastTimestamp" to System.currentTimeMillis()
                )

                // merge() here matters: without it, this set() call
                // silently overwrites the whole doc on every message sent --
                // including the typingUsers field maintained above.
                db.collection("privateChats")
                    .document(chatId)
                    .set(chatData, SetOptions.merge())

                etMessage.text.clear()

                typingHandler.removeCallbacks(stopTypingRunnable)
                setTypingStatus(false)

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
