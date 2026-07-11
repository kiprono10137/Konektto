package com.example.konektto.konektto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.konektto.R
import com.example.konektto.konektto.models.IncomingFriendRequest

class FriendRequestAdapter(
    private val requests: List<IncomingFriendRequest>,
    private val onAccept: (IncomingFriendRequest) -> Unit,
    private val onDecline: (IncomingFriendRequest) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imgAvatar: ImageView =
            itemView.findViewById(R.id.imgRequesterAvatar)

        val tvName: TextView =
            itemView.findViewById(R.id.tvRequesterName)

        val btnAccept: Button =
            itemView.findViewById(R.id.btnAccept)

        val btnDecline: Button =
            itemView.findViewById(R.id.btnDecline)

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RequestViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.friend_request_item, parent, false)

        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RequestViewHolder,
        position: Int
    ) {

        val request = requests[position]

        holder.tvName.text =
            request.senderUsername.ifBlank { "Unknown User" }

        if (request.senderProfileImage.isNotBlank()) {

            Glide.with(holder.imgAvatar.context)
                .load(request.senderProfileImage)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon)
                .circleCrop()
                .into(holder.imgAvatar)

        } else {

            holder.imgAvatar.setImageResource(
                android.R.drawable.sym_def_app_icon
            )

        }

        holder.btnAccept.setOnClickListener {
            onAccept(request)
        }

        holder.btnDecline.setOnClickListener {
            onDecline(request)
        }

    }

    override fun getItemCount() = requests.size
}
