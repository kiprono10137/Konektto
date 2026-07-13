package com.example.konektto.konektto.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.konektto.R
import com.example.konektto.konektto.models.Message
import com.example.konektto.konektto.utils.AttachmentUploader
import com.example.konektto.konektto.utils.AudioPlaybackManager
import com.example.konektto.konektto.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messageList: List<Message>,
    private val viewerRole: String,
    private val onDeleteClick: (Message) -> Unit,
    private val onPinClick: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    // Tap-to-reveal state for flagged messages, keyed by messageId rather
    // than position -- a message that's been revealed should stay
    // revealed if it scrolls off-screen and back, which position-based
    // state on a recycled ViewHolder would not survive.
    private val revealedMessageIds = mutableSetOf<String>()

    inner class MessageViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvSenderName: TextView =
            itemView.findViewById(R.id.tvSenderName)

        val tvMessage: TextView =
            itemView.findViewById(R.id.tvMessage)

        val tvTime: TextView =
            itemView.findViewById(R.id.tvTime)

        val imgAttachment: ImageView =
            itemView.findViewById(R.id.imgAttachment)

        val audioPlayerRow: View =
            itemView.findViewById(R.id.audioPlayerRow)

        val btnPlayAudio: Button =
            itemView.findViewById(R.id.btnPlayAudio)

        val tvAudioDuration: TextView =
            itemView.findViewById(R.id.tvAudioDuration)

        val fileCard: View =
            itemView.findViewById(R.id.fileCard)

        val tvFileName: TextView =
            itemView.findViewById(R.id.tvFileName)

        val tvFileSize: TextView =
            itemView.findViewById(R.id.tvFileSize)

        init {

            itemView.setOnLongClickListener {

                val message = messageList[bindingAdapterPosition]

                val currentUser =
                    FirebaseAuth.getInstance().currentUser

                val isModerator = viewerRole == "owner" || viewerRole == "moderator"
                val isOwnMessage = currentUser?.uid == message.senderId

                if (isOwnMessage || isModerator) {

                    val popup = PopupMenu(
                        itemView.context,
                        itemView
                    )

                    // Anyone can delete their own message; owners and
                    // moderators can delete anyone's, which is the whole
                    // point of having moderation in the first place.
                    if (isOwnMessage || isModerator) {
                        popup.menu.add("Delete")
                    }

                    if (isModerator) {
                        popup.menu.add("📌 Pin to top")
                    }

                    popup.setOnMenuItemClickListener { item ->

                        when (item.title) {
                            "Delete" -> onDeleteClick(message)
                            "📌 Pin to top" -> onPinClick(message)
                        }

                        true
                    }

                    popup.show()

                }

                true

            }

        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MessageViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_item, parent, false)

        return MessageViewHolder(view)

    }

    override fun onBindViewHolder(
        holder: MessageViewHolder,
        position: Int
    ) {

        val message = messageList[position]

        holder.tvSenderName.text = message.senderName

        // Hard reset every attachment slot on every bind -- a recycled
        // view showing a stale image/file/audio row from whatever message
        // previously occupied it is a real RecyclerView bug, not a
        // hypothetical one.
        holder.imgAttachment.visibility = View.GONE
        holder.audioPlayerRow.visibility = View.GONE
        holder.fileCard.visibility = View.GONE
        holder.btnPlayAudio.text = "▶"

        when (message.attachmentType) {

            "image", "gif" -> {

                holder.imgAttachment.visibility = View.VISIBLE

                Glide.with(holder.imgAttachment.context)
                    .load(message.attachmentUrl)
                    .into(holder.imgAttachment)

                holder.imgAttachment.setOnClickListener {

                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(message.attachmentUrl)
                    )

                    holder.imgAttachment.context.startActivity(intent)

                }

            }

            "audio" -> {

                holder.audioPlayerRow.visibility = View.VISIBLE
                holder.tvAudioDuration.text = TimeUtils.formatDuration(message.attachmentDuration)

                holder.btnPlayAudio.text =
                    if (AudioPlaybackManager.isPlaying(message.attachmentUrl)) "⏸" else "▶"

                holder.btnPlayAudio.setOnClickListener {

                    if (AudioPlaybackManager.isPlaying(message.attachmentUrl)) {

                        AudioPlaybackManager.stop()
                        holder.btnPlayAudio.text = "▶"

                    } else {

                        holder.btnPlayAudio.text = "⏸"

                        AudioPlaybackManager.play(
                            url = message.attachmentUrl,
                            onCompletion = { holder.btnPlayAudio.text = "▶" },
                            onStopped = { holder.btnPlayAudio.text = "▶" }
                        )

                    }

                }

            }

            "file" -> {

                holder.fileCard.visibility = View.VISIBLE
                holder.tvFileName.text = message.attachmentName.ifBlank { "file" }
                holder.tvFileSize.text = AttachmentUploader.formatFileSize(message.attachmentSize)

                holder.fileCard.setOnClickListener {

                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(message.attachmentUrl)
                    )

                    holder.fileCard.context.startActivity(intent)

                }

            }

        }

        if (message.text.isBlank() && message.attachmentType.isNotBlank()) {

            holder.tvMessage.visibility = View.GONE

        } else {

            holder.tvMessage.visibility = View.VISIBLE

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isOwnMessage = currentUserId == message.senderId
            val isModerator = viewerRole == "owner" || viewerRole == "moderator"
            val isRevealed = message.messageId in revealedMessageIds

            when {

                // The message's own sender and moderators always see the
                // real text -- hiding a flagged message from the person
                // who wrote it, or from the person whose job is to act on
                // it, defeats the point. A warning badge tells them it's
                // been flagged without actually hiding anything.
                message.flagged && (isOwnMessage || isModerator) -> {

                    holder.tvMessage.text = "⚠️ ${message.text}"

                }

                // Everyone else sees a placeholder instead of the message
                // itself, until they explicitly tap through -- soft
                // friction rather than a silent, total hide, and it
                // stays revealed once tapped (tracked by messageId, not
                // position, so scrolling away and back doesn't re-hide it).
                message.flagged && !isRevealed -> {

                    holder.tvMessage.text =
                        "⚠️ Message hidden — flagged as potentially inappropriate. Tap to view."

                    holder.tvMessage.setOnClickListener {

                        revealedMessageIds.add(message.messageId)
                        notifyItemChanged(holder.bindingAdapterPosition)

                    }

                }

                // Once revealed (or if never flagged in the first place),
                // show the real text -- but a revealed message still
                // carries the same small warning marker as the sender/
                // moderator view, so it's clear why it was hidden rather
                // than just silently looking like any other message now.
                message.flagged && isRevealed -> {

                    holder.tvMessage.text = "⚠️ ${message.text}"
                    holder.tvMessage.setOnClickListener(null)

                }

                else -> {

                    holder.tvMessage.text = message.text
                    holder.tvMessage.setOnClickListener(null)

                }

            }

        }

        val sdf =
            SimpleDateFormat("hh:mm a", Locale.getDefault())

        holder.tvTime.text =
            sdf.format(Date(message.timestamp))

    }

    override fun getItemCount() = messageList.size
}
