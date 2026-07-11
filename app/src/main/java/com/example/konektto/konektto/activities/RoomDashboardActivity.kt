package com.example.konektto.konektto.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.adapters.MessageAdapter
import com.example.konektto.konektto.fragments.AttachmentPickerSheet
import com.example.konektto.konektto.models.Message
import com.example.konektto.konektto.utils.AttachmentUploader
import com.example.konektto.konektto.utils.AudioPlaybackManager
import com.example.konektto.konektto.utils.VoiceRecorderDialog
import com.example.konektto.konektto.widgets.GifSupportEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RoomDashboardActivity : AppCompatActivity() {

    private lateinit var tvRoomTitle: TextView
    private lateinit var tvCreator: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvMembers: TextView

    private lateinit var tvAnnouncement: TextView

    private lateinit var pinnedMessageBanner: View
    private lateinit var tvPinnedMessage: TextView
    private lateinit var btnUnpin: Button

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: GifSupportEditText

    private lateinit var btnSend: Button
    private lateinit var btnAttach: Button
    private lateinit var btnViewMembers: Button
    private lateinit var btnInvite: Button
    private lateinit var btnLeaveRoom: Button
    private lateinit var btnAdminPanel: Button

    private lateinit var adapter: MessageAdapter
    private lateinit var db: FirebaseFirestore

    private val messageList = mutableListOf<Message>()

    private lateinit var roomId: String
    private var viewerRole: String = "member"
    private var lastPinnedText: String = ""
    private var inviteCode: String = ""

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { sendImageOrGifAttachment(it) }
        }

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { sendFileAttachment(it) }
        }

    private val recordAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchVoiceRecorder()
            } else {
                Toast.makeText(this, "Microphone permission is needed to record a voice note.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_dashboard)

        supportActionBar?.hide()

        tvRoomTitle = findViewById(R.id.tvRoomTitle)
        tvCreator = findViewById(R.id.tvCreator)
        tvCategory = findViewById(R.id.tvCategory)
        tvMembers = findViewById(R.id.tvMembers)
        tvAnnouncement = findViewById(R.id.tvAnnouncement)

        pinnedMessageBanner = findViewById(R.id.pinnedMessageBanner)
        tvPinnedMessage = findViewById(R.id.tvPinnedMessage)
        btnUnpin = findViewById(R.id.btnUnpin)


        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)

        btnSend = findViewById(R.id.btnSend)
        btnAttach = findViewById(R.id.btnAttach)
        btnViewMembers = findViewById(R.id.btnViewMembers)
        btnInvite = findViewById(R.id.btnInvite)
        btnLeaveRoom = findViewById(R.id.btnLeaveRoom)
        btnAdminPanel = findViewById(R.id.btnAdminPanel)

        db = FirebaseFirestore.getInstance()

        roomId = intent.getStringExtra("roomId") ?: ""

        rvMessages.layoutManager = LinearLayoutManager(this)

        loadViewerRoleThenSetupChat()

        loadRoomDetails()
        loadAnnouncement()

        btnUnpin.setOnClickListener {
            unpinMessage()
        }

        btnSend.setOnClickListener {
            sendMessage()
        }

        btnAttach.setOnClickListener {

            AttachmentPickerSheet(
                onPickImage = { imagePicker.launch("image/*") },
                onPickVoiceNote = { requestVoiceNote() },
                onPickFile = { filePicker.launch("*/*") }
            ).show(supportFragmentManager, "attachment_picker")

        }

        etMessage.onContentCommitted = { uri, _ ->
            sendImageOrGifAttachment(uri)
        }

        btnViewMembers.setOnClickListener {

            val intent = Intent(
                this,
                MembersActivity::class.java
            )

            intent.putExtra("roomId", roomId)

            startActivity(intent)

        }

        btnInvite.setOnClickListener {
            shareInvite()
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

    override fun onDestroy() {
        super.onDestroy()
        AudioPlaybackManager.stop()
    }

    /**
     * The message adapter's own moderation menu (delete-any-message,
     * pin-to-top) depends on knowing the current user's role in this room
     * first -- can't build it until that comes back, same reasoning as
     * MembersActivity.
     */
    private fun loadViewerRoleThenSetupChat() {

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId == null) {
            setupChatAdapter()
            return
        }

        db.collection("rooms")
            .document(roomId)
            .collection("members")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->

                viewerRole = document.getString("role") ?: "member"
                setupChatAdapter()
                updateUnpinButtonVisibility()

            }
            .addOnFailureListener {

                setupChatAdapter()

            }

    }

    private fun setupChatAdapter() {

        adapter = MessageAdapter(
            messageList = messageList,
            viewerRole = viewerRole,
            onDeleteClick = { message -> deleteMessage(message) },
            onPinClick = { message -> pinMessage(message) }
        )

        rvMessages.adapter = adapter

        listenForMessages()

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

                inviteCode = document.getString("inviteCode") ?: ""

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

        withCurrentUsername { uid, username ->

            val messageRef = db.collection("rooms")
                .document(roomId)
                .collection("messages")
                .document()

            val message = Message(
                messageId = messageRef.id,
                senderId = uid,
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

    }

    /**
     * A GIF from the keyboard and a photo from the gallery are, from here
     * on, identical: both are just a Uri that needs uploading and a
     * message that needs sending. AttachmentUploader already tells the
     * two apart internally by MIME type.
     */
    private fun sendImageOrGifAttachment(uri: Uri) {

        Toast.makeText(this, "Sending...", Toast.LENGTH_SHORT).show()

        AttachmentUploader.uploadImageOrGif(this, uri) { url ->

            if (url == null) {

                Toast.makeText(this, "Upload failed.", Toast.LENGTH_LONG).show()
                return@uploadImageOrGif

            }

            val mimeType = contentResolver.getType(uri) ?: ""
            val attachmentType = if (mimeType == "image/gif") "gif" else "image"

            withCurrentUsername { uid, username ->

                val messageRef = db.collection("rooms")
                    .document(roomId)
                    .collection("messages")
                    .document()

                val message = Message(
                    messageId = messageRef.id,
                    senderId = uid,
                    senderName = username,
                    text = "",
                    timestamp = System.currentTimeMillis(),
                    attachmentType = attachmentType,
                    attachmentUrl = url
                )

                messageRef.set(message)
                    .addOnFailureListener { e ->

                        Toast.makeText(
                            this,
                            e.localizedMessage,
                            Toast.LENGTH_LONG
                        ).show()

                    }

            }

        }

    }

    /**
     * Every send path (text, image, gif, and eventually voice/file) needs
     * the sender's current username looked up first -- pulling that into
     * one helper instead of repeating the same Firestore lookup + null
     * checks in every send function.
     */
    private fun withCurrentUsername(action: (uid: String, username: String) -> Unit) {

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

                action(currentUser.uid, username)

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()

            }

    }

    private fun requestVoiceNote() {

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            launchVoiceRecorder()
        } else {
            recordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

    }

    private fun launchVoiceRecorder() {

        VoiceRecorderDialog.show(this) { filePath, durationMs ->
            sendVoiceNoteAttachment(filePath, durationMs)
        }

    }

    private fun sendVoiceNoteAttachment(filePath: String, durationMs: Long) {

        Toast.makeText(this, "Sending voice note...", Toast.LENGTH_SHORT).show()

        AttachmentUploader.uploadAudio(filePath) { url ->

            if (url == null) {
                Toast.makeText(this, "Upload failed.", Toast.LENGTH_LONG).show()
                return@uploadAudio
            }

            withCurrentUsername { uid, username ->

                val messageRef = db.collection("rooms")
                    .document(roomId)
                    .collection("messages")
                    .document()

                val message = Message(
                    messageId = messageRef.id,
                    senderId = uid,
                    senderName = username,
                    text = "",
                    timestamp = System.currentTimeMillis(),
                    attachmentType = "audio",
                    attachmentUrl = url,
                    attachmentDuration = durationMs
                )

                messageRef.set(message)
                    .addOnFailureListener { e ->

                        Toast.makeText(
                            this,
                            e.localizedMessage,
                            Toast.LENGTH_LONG
                        ).show()

                    }

            }

        }

    }

    private fun sendFileAttachment(uri: Uri) {

        Toast.makeText(this, "Sending file...", Toast.LENGTH_SHORT).show()

        AttachmentUploader.uploadGenericFile(this, uri) { url, filename, sizeBytes ->

            if (url == null) {
                Toast.makeText(this, "Upload failed.", Toast.LENGTH_LONG).show()
                return@uploadGenericFile
            }

            withCurrentUsername { uid, username ->

                val messageRef = db.collection("rooms")
                    .document(roomId)
                    .collection("messages")
                    .document()

                val message = Message(
                    messageId = messageRef.id,
                    senderId = uid,
                    senderName = username,
                    text = "",
                    timestamp = System.currentTimeMillis(),
                    attachmentType = "file",
                    attachmentUrl = url,
                    attachmentName = filename,
                    attachmentSize = sizeBytes
                )

                messageRef.set(message)
                    .addOnFailureListener { e ->

                        Toast.makeText(
                            this,
                            e.localizedMessage,
                            Toast.LENGTH_LONG
                        ).show()

                    }

            }

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

                    val pinnedText = snapshot.getString("pinnedMessageText") ?: ""
                    val pinnedSender = snapshot.getString("pinnedMessageSenderName") ?: ""

                    lastPinnedText = pinnedText

                    if (pinnedText.isNotEmpty()) {

                        pinnedMessageBanner.visibility = View.VISIBLE
                        tvPinnedMessage.text = "📌 $pinnedSender: $pinnedText"
                        updateUnpinButtonVisibility()

                    } else {

                        pinnedMessageBanner.visibility = View.GONE

                    }

                }

            }

    }

    private fun shareInvite() {

        if (inviteCode.isBlank()) {

            Toast.makeText(
                this,
                "This community doesn't have an invite code yet.",
                Toast.LENGTH_SHORT
            ).show()

            return

        }

        // The deep link is included for keyboards/apps that do auto-linkify
        // custom URI schemes, but the plain code is the reliable fallback --
        // most messaging apps only auto-linkify http/https, not custom
        // schemes like konektto://, so a pasted link might just sit there
        // as inert text depending on where it's shared.
        val shareText = buildString {
            append("Join \"${tvRoomTitle.text}\" on Konektto!\n\n")
            append("Tap this link: konektto://join/$inviteCode\n\n")
            append("Or open Konektto, tap \"Join by Code\", and enter: $inviteCode")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Share invite"))

    }

    private fun updateUnpinButtonVisibility() {

        // Only owners and moderators can unpin -- everyone else just sees
        // what's pinned, same as they could see it was pinned in the
        // first place without being the one who did it.
        val canModerate = viewerRole == "owner" || viewerRole == "moderator"

        btnUnpin.visibility =
            if (canModerate && lastPinnedText.isNotEmpty()) View.VISIBLE else View.GONE

    }

    private fun pinMessage(message: Message) {

        val previewText = when {

            message.text.isNotBlank() -> message.text

            message.attachmentType == "image" -> "📷 Photo"
            message.attachmentType == "gif" -> "GIF"
            message.attachmentType == "audio" -> "🎤 Voice note"
            message.attachmentType == "file" -> "📎 ${message.attachmentName.ifBlank { "File" }}"

            else -> ""

        }

        db.collection("rooms")
            .document(roomId)
            .update(
                mapOf(
                    "pinnedMessageId" to message.messageId,
                    "pinnedMessageText" to previewText,
                    "pinnedMessageSenderName" to message.senderName
                )
            )
            .addOnSuccessListener {

                Toast.makeText(this, "Message pinned.", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e ->

                Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()

            }

    }

    private fun unpinMessage() {

        db.collection("rooms")
            .document(roomId)
            .update(
                mapOf(
                    "pinnedMessageId" to "",
                    "pinnedMessageText" to "",
                    "pinnedMessageSenderName" to ""
                )
            )

    }

}