package com.example.konektto.konektto.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.models.PrivateMessage
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

        holder.tvMessage.text = message.text

        val sdf =
            SimpleDateFormat("hh:mm a", Locale.getDefault())

        holder.tvTime.text =
            sdf.format(Date(message.timestamp))

        // Align my messages to the right, theirs to the left -- the one
        // visual cue every messaging app relies on to make a conversation
        // scannable at a glance.
        val params = holder.cardMessage.layoutParams as FrameLayout.LayoutParams
        params.gravity = if (isMine) Gravity.END else Gravity.START
        holder.cardMessage.layoutParams = params

        val bubbleColor: Int
        val textColor: Int
        val timeColor: Int

        if (isMine) {

            bubbleColor = MaterialColors.getColor(holder.cardMessage, com.google.android.material.R.attr.colorPrimary)
            textColor = MaterialColors.getColor(holder.cardMessage, com.google.android.material.R.attr.colorOnPrimary)
            timeColor = textColor

        } else {

            bubbleColor = MaterialColors.getColor(holder.cardMessage, com.google.android.material.R.attr.colorSurfaceVariant)
            textColor = MaterialColors.getColor(holder.cardMessage, com.google.android.material.R.attr.colorOnSurfaceVariant)
            timeColor = textColor

        }

        holder.cardMessage.setCardBackgroundColor(bubbleColor)
        holder.tvMessage.setTextColor(textColor)
        holder.tvTime.setTextColor(timeColor)

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
