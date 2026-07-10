package com.example.konektto.konektto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.models.Room

class RoomAdapter(
    private val rooms: List<Room>,
    private val onRoomClick: (Room) -> Unit
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvRoomName: TextView = itemView.findViewById(R.id.tvRoomName)
        val tvRoomDescription: TextView = itemView.findViewById(R.id.tvRoomDescription)
        val tvMemberCount: TextView = itemView.findViewById(R.id.tvMemberCount)

        init {
            itemView.setOnClickListener {

                val position = bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION) {
                    onRoomClick(rooms[position])
                }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.room_item, parent, false)

        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {

        val room = rooms[position]

        holder.tvRoomName.text = room.roomName
        holder.tvRoomDescription.text = room.roomDescription

        holder.tvMemberCount.text =
            "👥 ${room.memberCount} Members"

        holder.tvCategory.text = when (room.category) {

            "Football" -> "⚽ Football"
            "Gaming" -> "🎮 Gaming"
            "Music" -> "🎵 Music"
            "Programming" -> "💻 Programming"
            "Technology" -> "📱 Technology"
            "Business" -> "💼 Business"
            "Fashion" -> "👕 Fashion"
            "Movies" -> "🎬 Movies"
            "Relationships" -> "❤️ Relationships"
            "Education" -> "📚 Education"

            else -> "💬 General"
        }
    }

    override fun getItemCount(): Int = rooms.size
}