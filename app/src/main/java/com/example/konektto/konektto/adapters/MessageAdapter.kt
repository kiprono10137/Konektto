package com.example.konektto.konektto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.models.Message
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
        holder.tvMessage.text = message.text

        val sdf =
            SimpleDateFormat("hh:mm a", Locale.getDefault())

        holder.tvTime.text =
            sdf.format(Date(message.timestamp))

    }

    override fun getItemCount() = messageList.size
}