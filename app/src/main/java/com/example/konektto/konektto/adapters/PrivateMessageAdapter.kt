package com.example.konektto.konektto.adapters

import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.konektto.R
import com.example.konektto.konektto.models.PrivateMessage
import com.example.konektto.konektto.utils.AttachmentUploader
import com.example.konektto.konektto.utils.AudioPlaybackManager
import com.example.konektto.konektto.utils.TimeUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.*

class PrivateMessageAdapter(
    private val messages: List<PrivateMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<PrivateMessageAdapter.PrivateMessageViewHolder>() {

    inner class PrivateMessageViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val cardMessage: MaterialCardView =
            itemView.findViewById(R.id.cardMessage)

        val tvMessage: TextView =
            itemView.findViewById(R.id.tvPrivateMessage)

        val tvTime: TextView =
            itemView.findViewById(R.id.tvPrivateTime)

        val tvReadReceipt: TextView =
            itemView.findViewById(R.id.tvReadReceipt)

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

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PrivateMessageViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.private_message_item,
                parent,
                false
            )

        return PrivateMessageViewHolder(view)

    }

    override fun onBindViewHolder(
        holder: PrivateMessageViewHolder,
        position: Int
    ) {

        val message = messages[position]
        val isMine = message.senderId == currentUserId

        // Align my messages to the right, theirs to the left -- the one
        // visual cue every messaging app relies on to make a conversation
        // scannable at a glance.
        val params = holder.cardMessage.layoutParams as FrameLayout.LayoutParams
        params.gravity = if (isMine) Gravity.END else Gravity.START
        holder.cardMessage.layoutParams = params

        val bubbleColor: Int
        val textColor: Int

        if (isMine) {

            bubbleColor = MaterialColors.getColor(holder.cardMessage, android.R.attr.colorPrimary)
            textColor = MaterialColors.getColor(holder.cardMessage, com.google.android.material.R.attr.colorOnPrimary)

        } else {

            bubbleColor = MaterialColors.getColor(holder.cardMessage, com.google.android.material.R.attr.colorSurfaceVariant)
            textColor = MaterialColors.getColor(holder.cardMessage, com.google.android.material.R.attr.colorOnSurfaceVariant)

        }

        holder.cardMessage.setCardBackgroundColor(bubbleColor)
        holder.tvMessage.setTextColor(textColor)
        holder.tvTime.setTextColor(textColor)

        // Hard reset every attachment slot on every bind. This view may
        // have just been recycled from a completely different message --
        // if item #3 was a photo and this view is now showing item #9
        // (text-only), failing to hide imgAttachment here would leave
        // item #3's photo visibly stuck on item #9's bubble.
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
                holder.tvAudioDuration.setTextColor(textColor)
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
                holder.tvFileName.setTextColor(textColor)
                holder.tvFileSize.text = AttachmentUploader.formatFileSize(message.attachmentSize)
                holder.tvFileSize.setTextColor(textColor)

                holder.fileCard.setOnClickListener {

                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(message.attachmentUrl)
                    )

                    holder.fileCard.context.startActivity(intent)

                }

            }

        }

        // A caption is optional on an attachment, but an attachment isn't
        // optional on a caption -- if there's no text and no attachment,
        // something's wrong with the data, but we still shouldn't render
        // an empty bubble taking up space for no reason.
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

        // Read receipts only make sense on messages *I* sent -- nobody
        // expects to see a checkmark on a bubble someone else sent them.
        if (isMine) {

            holder.tvReadReceipt.visibility = View.VISIBLE
            holder.tvReadReceipt.text = if (message.read) "✓✓" else "✓"
            holder.tvReadReceipt.setTextColor(textColor)

        } else {

            holder.tvReadReceipt.visibility = View.GONE

        }

    }

    override fun getItemCount() = messages.size

}
