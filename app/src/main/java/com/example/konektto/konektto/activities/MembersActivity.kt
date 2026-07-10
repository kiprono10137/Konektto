package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.adapters.MemberAdapter
import com.example.konektto.konektto.models.Member
import com.google.firebase.firestore.FirebaseFirestore

class MembersActivity : AppCompatActivity() {

    private lateinit var rvMembers: RecyclerView
    private lateinit var adapter: MemberAdapter

    private lateinit var db: FirebaseFirestore

    private val memberList = mutableListOf<Member>()

    private lateinit var roomId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_members)

        supportActionBar?.hide()

        rvMembers = findViewById(R.id.rvMembers)

        db = FirebaseFirestore.getInstance()

        roomId = intent.getStringExtra("roomId") ?: ""

        adapter = MemberAdapter(memberList) { member ->

            val intent = Intent(
                this,
                UserProfileActivity::class.java
            )

            intent.putExtra("userId", member.userId)

            startActivity(intent)

        }

        rvMembers.layoutManager = LinearLayoutManager(this)
        rvMembers.adapter = adapter

        loadMembers()
    }

    private fun loadMembers() {

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
}