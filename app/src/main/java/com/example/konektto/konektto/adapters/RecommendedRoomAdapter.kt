package com.example.konektto.konektto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.models.RecommendedRoom

class RecommendedRoomAdapter(
    private val items: List<RecommendedRoom>,
    private val onClick: (RecommendedRoom) -> Unit
) : RecyclerView.Adapter<RecommendedRoomAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvName: TextView = itemView.findViewById(R.id.tvRecRoomName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvRecCategory)
        val tvReason: TextView = itemView.findViewById(R.id.tvRecReason)

        init {
            itemView.setOnClickListener {

                val position = bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION) {
                    onClick(items[position])
                }

            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recommended_room_item, parent, false)

        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = items[position]

        holder.tvName.text = item.room.roomName.ifBlank { "Community" }

        val category = item.room.category
        if (category.isNotBlank()) {
            holder.tvCategory.visibility = View.VISIBLE
            holder.tvCategory.text = category
        } else {
            holder.tvCategory.visibility = View.GONE
        }

        holder.tvReason.text = item.reason

    }

    override fun getItemCount() = items.size

}
