package com.example.konektto.konektto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.models.Member

class MemberAdapter(
    private val members: List<Member>,
    private val onMemberClick: (Member) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvUsername: TextView =
            itemView.findViewById(R.id.tvUsername)

        init {
            itemView.setOnClickListener {

                val position = bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION) {
                    onMemberClick(members[position])
                }

            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MemberViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.member_item, parent, false)

        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MemberViewHolder,
        position: Int
    ) {

        holder.tvUsername.text = members[position].username

    }

    override fun getItemCount() = members.size
}