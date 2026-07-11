package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.adapters.MemberAdapter
import com.example.konektto.konektto.models.Member
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MembersActivity : AppCompatActivity() {

    private lateinit var rvMembers: RecyclerView
    private lateinit var adapter: MemberAdapter

    private lateinit var db: FirebaseFirestore

    private val memberList = mutableListOf<Member>()

    private lateinit var roomId: String
    private var viewerRole: String = "member"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_members)

        supportActionBar?.hide()

        rvMembers = findViewById(R.id.rvMembers)

        db = FirebaseFirestore.getInstance()

        roomId = intent.getStringExtra("roomId") ?: ""

        rvMembers.layoutManager = LinearLayoutManager(this)

        loadViewerRoleThenMembers()

    }

    /**
     * The member list's own moderation actions depend on knowing the
     * current user's role in *this* room first -- an owner viewing the
     * list needs different buttons than a plain member would, so the
     * adapter can't be built until this comes back.
     */
    private fun loadViewerRoleThenMembers() {

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId == null) {
            loadMembers()
            return
        }

        db.collection("rooms")
            .document(roomId)
            .collection("members")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->

                viewerRole = document.getString("role") ?: "member"
                loadMembers()

            }
            .addOnFailureListener {

                loadMembers()

            }

    }

    private fun loadMembers() {

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        adapter = MemberAdapter(
            members = memberList,
            viewerUserId = currentUserId,
            viewerRole = viewerRole,
            onMemberClick = { member ->

                val intent = Intent(this, UserProfileActivity::class.java)
                intent.putExtra("userId", member.userId)
                startActivity(intent)

            },
            onPromote = { member -> updateRole(member, "moderator") },
            onDemote = { member -> updateRole(member, "member") },
            onRemove = { member -> confirmRemoveMember(member) }
        )

        rvMembers.adapter = adapter

        db.collection("rooms")
            .document(roomId)
            .collection("members")
            .orderBy("joinedAt")
            .get()
            .addOnSuccessListener { documents ->

                memberList.clear()

                for (document in documents) {

                    val member = document.toObject(Member::class.java)
                    memberList.add(member)

                }

                // Owner first, then moderators, then everyone else --
                // the people with the most authority over the community
                // are the people worth seeing first when checking who's in it.
                memberList.sortBy {
                    when (it.role) {
                        "owner" -> 0
                        "moderator" -> 1
                        else -> 2
                    }
                }

                adapter.notifyDataSetChanged()

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()

            }

    }

    private fun updateRole(member: Member, newRole: String) {

        db.collection("rooms")
            .document(roomId)
            .collection("members")
            .document(member.userId)
            .update("role", newRole)
            .addOnSuccessListener {

                val index = memberList.indexOfFirst { it.userId == member.userId }

                if (index != -1) {
                    memberList[index] = memberList[index].copy(role = newRole)
                    memberList.sortBy {
                        when (it.role) {
                            "owner" -> 0
                            "moderator" -> 1
                            else -> 2
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                val message = if (newRole == "moderator")
                    "${member.username} is now a moderator."
                else
                    "${member.username} is now a regular member."

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()

            }

    }

    private fun confirmRemoveMember(member: Member) {

        AlertDialog.Builder(this)
            .setTitle("Remove Member")
            .setMessage("Remove ${member.username} from this community?")
            .setPositiveButton("Remove") { _, _ -> removeMember(member) }
            .setNegativeButton("Cancel", null)
            .show()

    }

    private fun removeMember(member: Member) {

        val roomRef = db.collection("rooms").document(roomId)

        roomRef.collection("members")
            .document(member.userId)
            .delete()
            .addOnSuccessListener {

                roomRef.get().addOnSuccessListener { document ->

                    val currentCount = document.getLong("memberCount") ?: 1
                    val newCount = if (currentCount > 0) currentCount - 1 else 0

                    roomRef.update("memberCount", newCount)

                }

                memberList.removeAll { it.userId == member.userId }
                adapter.notifyDataSetChanged()

                Toast.makeText(
                    this,
                    "${member.username} was removed from the community.",
                    Toast.LENGTH_SHORT
                ).show()

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()

            }

    }

}
