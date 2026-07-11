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
    private val onDeleteClick: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

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

                if (currentUser?.uid == message.senderId) {

                    val popup = PopupMenu(
                        itemView.context,
                        itemView
                    )

                    popup.menu.add("Delete")

                    popup.setOnMenuItemClickListener {

                        onDeleteClick(message)

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
            holder.tvMessage.text = message.text

        }

        val sdf =
            SimpleDateFormat("hh:mm a", Locale.getDefault())

        holder.tvTime.text =
            sdf.format(Date(message.timestamp))

    }

    override fun getItemCount() = messageList.size
}
