package com.example.konektto.konektto.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.adapters.FriendRequestAdapter
import com.example.konektto.konektto.models.IncomingFriendRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var rvFriendRequests: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: FriendRequestAdapter

    private val requestList = mutableListOf<IncomingFriendRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_requests)

        supportActionBar?.hide()

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvFriendRequests = findViewById(R.id.rvFriendRequests)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        adapter = FriendRequestAdapter(
            requestList,
            onAccept = { request -> acceptRequest(request) },
            onDecline = { request -> declineRequest(request) }
        )

        rvFriendRequests.layoutManager = LinearLayoutManager(this)
        rvFriendRequests.adapter = adapter

        loadRequests()

    }

    private fun loadRequests() {

        val myId = auth.currentUser?.uid ?: return

        db.collection("friendRequests")
            .whereEqualTo("receiverId", myId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->

                requestList.clear()

                if (documents.isEmpty) {
                    updateEmptyState()
                    return@addOnSuccessListener
                }

                var remaining = documents.size()

                for (document in documents) {

                    val senderId = document.getString("senderId") ?: ""

                    db.collection("users")
                        .document(senderId)
                        .get()
                        .addOnSuccessListener { userDoc ->

                            requestList.add(
                                IncomingFriendRequest(
                                    requestId = document.id,
                                    senderId = senderId,
                                    senderUsername =
                                        userDoc.getString("username") ?: "Unknown User",
                                    senderProfileImage =
                                        userDoc.getString("profileImage") ?: ""
                                )
                            )

                            remaining--

                            if (remaining == 0) {
                                adapter.notifyDataSetChanged()
                                updateEmptyState()
                            }

                        }
                        .addOnFailureListener {

                            remaining--

                            if (remaining == 0) {
                                adapter.notifyDataSetChanged()
                                updateEmptyState()
                            }

                        }

                }

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    e.localizedMessage ?: "Failed to load friend requests",
                    Toast.LENGTH_LONG
                ).show()

            }

    }

    private fun acceptRequest(request: IncomingFriendRequest) {

        db.collection("friendRequests")
            .document(request.requestId)
            .update("status", "accepted")
            .addOnSuccessListener {

                removeFromList(request)

                Toast.makeText(
                    this,
                    "You are now friends with ${request.senderUsername}",
                    Toast.LENGTH_SHORT
                ).show()

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    e.localizedMessage ?: "Failed to accept request",
                    Toast.LENGTH_LONG
                ).show()

            }

    }

    private fun declineRequest(request: IncomingFriendRequest) {

        db.collection("friendRequests")
            .document(request.requestId)
            .delete()
            .addOnSuccessListener {
                removeFromList(request)
            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    e.localizedMessage ?: "Failed to decline request",
                    Toast.LENGTH_LONG
                ).show()

            }

    }

    private fun removeFromList(request: IncomingFriendRequest) {

        val position = requestList.indexOf(request)

        if (position != -1) {
            requestList.removeAt(position)
            adapter.notifyItemRemoved(position)
        }

        updateEmptyState()

    }

    private fun updateEmptyState() {

        tvEmptyState.visibility =
            if (requestList.isEmpty()) View.VISIBLE else View.GONE

    }

}
