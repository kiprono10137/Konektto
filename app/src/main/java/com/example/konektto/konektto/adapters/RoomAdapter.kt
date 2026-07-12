package com.example.konektto.konektto.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.models.Room
import com.example.konektto.konektto.utils.CategoryStyle

class RoomAdapter(
    private val rooms: List<Room>,
    private val onRoomClick: (Room) -> Unit
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val iconContainer: FrameLayout = itemView.findViewById(R.id.iconContainer)
        val tvIconEmoji: TextView = itemView.findViewById(R.id.tvIconEmoji)
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
            if (room.memberCount == 1) "1 Member" else "${room.memberCount} Members"

        val style = CategoryStyle.of(room.category)

        holder.tvIconEmoji.text = style.emoji
        holder.tvCategory.text = "${style.emoji} ${style.displayName}"

        // Every category gets its own color identity -- a list of ten
        // different communities should look alive and organized at a
        // glance, not like ten copies of the same purple pill.
        tintDrawable(holder.iconContainer.background, style.color)
        tintDrawable(holder.tvCategory.background, style.color)

    }

    override fun getItemCount(): Int = rooms.size

    private fun tintDrawable(drawable: android.graphics.drawable.Drawable?, color: Int) {

        val mutable = drawable?.mutate()

        if (mutable is GradientDrawable) {
            mutable.setColor(color)
        }

    }

}
