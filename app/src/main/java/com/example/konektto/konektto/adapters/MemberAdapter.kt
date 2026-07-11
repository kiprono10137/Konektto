package com.example.konektto.konektto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.models.Member

class MemberAdapter(
    private val members: List<Member>,
    private val viewerUserId: String,
    private val viewerRole: String,
    private val onMemberClick: (Member) -> Unit,
    private val onPromote: (Member) -> Unit,
    private val onDemote: (Member) -> Unit,
    private val onRemove: (Member) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvUsername: TextView =
            itemView.findViewById(R.id.tvUsername)

        val tvRoleBadge: TextView =
            itemView.findViewById(R.id.tvRoleBadge)

        val btnActions: Button =
            itemView.findViewById(R.id.btnMemberActions)

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

        val member = members[position]

        holder.tvUsername.text = member.username

        // A badge only earns its place for roles that aren't the default --
        // showing "Member" on every single row would just be noise.
        when (member.role) {

            "owner" -> {
                holder.tvRoleBadge.visibility = View.VISIBLE
                holder.tvRoleBadge.text = "👑 Owner"
            }

            "moderator" -> {
                holder.tvRoleBadge.visibility = View.VISIBLE
                holder.tvRoleBadge.text = "🛡️ Moderator"
            }

            else -> {
                holder.tvRoleBadge.visibility = View.GONE
            }

        }

        holder.btnActions.visibility = View.GONE
        holder.btnActions.setOnClickListener(null)

        val isSelf = member.userId == viewerUserId

        val canAct = when {

            isSelf -> false

            // Owners can act on moderators and plain members, but there's
            // only ever one owner, so there's nothing an owner needs to
            // do to another owner.
            viewerRole == "owner" && member.role != "owner" -> true

            // Moderators can only act on plain members -- never on the
            // owner, and never on each other.
            viewerRole == "moderator" && member.role == "member" -> true

            else -> false

        }

        if (canAct) {

            holder.btnActions.visibility = View.VISIBLE

            holder.btnActions.setOnClickListener {

                val popup = PopupMenu(holder.itemView.context, holder.btnActions)

                if (viewerRole == "owner") {

                    if (member.role == "moderator") {
                        popup.menu.add("Demote to Member")
                    } else {
                        popup.menu.add("Promote to Moderator")
                    }

                }

                popup.menu.add("Remove from Community")

                popup.setOnMenuItemClickListener { item ->

                    when (item.title) {

                        "Promote to Moderator" -> onPromote(member)
                        "Demote to Member" -> onDemote(member)
                        "Remove from Community" -> onRemove(member)

                    }

                    true

                }

                popup.show()

            }

        }

    }

    override fun getItemCount() = members.size
}
