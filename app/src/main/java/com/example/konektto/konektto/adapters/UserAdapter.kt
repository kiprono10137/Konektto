package com.example.konektto.konektto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.konektto.R
import com.example.konektto.konektto.models.UserProfile

class UserAdapter(
    private val users: List<UserProfile>,
    private val onUserClick: (UserProfile) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imgAvatar: ImageView =
            itemView.findViewById(R.id.imgUserAvatar)

        val tvUsername: TextView =
            itemView.findViewById(R.id.tvUserName)

        val tvBio: TextView =
            itemView.findViewById(R.id.tvUserBio)

        init {
            itemView.setOnClickListener {

                val position = bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(users[position])
                }

            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_item, parent, false)

        return UserViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: UserViewHolder,
        position: Int
    ) {

        val user = users[position]

        holder.tvUsername.text =
            user.username.ifBlank { "Unknown User" }

        holder.tvBio.text =
            user.bio.ifBlank { "No bio yet." }

        if (user.profileImage.isNotBlank()) {

            Glide.with(holder.imgAvatar.context)
                .load(user.profileImage)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon)
                .circleCrop()
                .into(holder.imgAvatar)

        } else {

            holder.imgAvatar.setImageResource(
                android.R.drawable.sym_def_app_icon
            )

        }

    }

    override fun getItemCount() = users.size
}
