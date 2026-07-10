package com.example.konektto.konektto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.models.PrivateMessage
import java.text.SimpleDateFormat
import java.util.*

class PrivateMessageAdapter(
    private val messages: List<PrivateMessage>
) : RecyclerView.Adapter<PrivateMessageAdapter.PrivateMessageViewHolder>() {

    inner class PrivateMessageViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvMessage: TextView =
            itemView.findViewById(R.id.tvPrivateMessage)

        val tvTime: TextView =
            itemView.findViewById(R.id.tvPrivateTime)

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

        holder.tvMessage.text = message.text

        val sdf =
            SimpleDateFormat("hh:mm a", Locale.getDefault())

        holder.tvTime.text =
            sdf.format(Date(message.timestamp))

    }

    override fun getItemCount() = messages.size

}
