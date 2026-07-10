package com.example.konektto.konektto.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.activities.RoomDashboardActivity
import com.example.konektto.konektto.adapters.RoomAdapter
import com.example.konektto.konektto.models.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RoomAdapter
    private lateinit var db: FirebaseFirestore

    private val roomList = mutableListOf<Room>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvRooms)

        db = FirebaseFirestore.getInstance()

        adapter = RoomAdapter(roomList) { room ->
            openRoom(room)
        }

        setupRecyclerView()
        loadUsername(view)
        loadRooms()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun loadUsername(view: View) {

        val txtWelcome = view.findViewById<TextView>(R.id.txtWelcome)

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                val username = document.getString("username") ?: "User"
                txtWelcome.text = "Welcome back, $username!"

            }
    }

    private fun loadRooms() {

        db.collection("rooms")
            .get()
            .addOnSuccessListener { documents ->

                roomList.clear()

                for (document in documents) {

                    val room = document.toObject(Room::class.java)
                    roomList.add(room)

                }

                adapter.notifyDataSetChanged()

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    requireContext(),
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()

            }
    }

    private fun openRoom(room: Room) {

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {

            Toast.makeText(
                requireContext(),
                "Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val memberRef = db.collection("rooms")
            .document(room.roomId)
            .collection("members")
            .document(currentUser.uid)

        memberRef.get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    openRoomDashboard(room)

                } else {

                    joinRoom(room, currentUser.uid)

                }

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    requireContext(),
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()

            }
    }

    private fun joinRoom(room: Room, userId: String) {

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDocument ->

                val username =
                    userDocument.getString("username") ?: "Unknown User"

                val memberData = hashMapOf(
                    "userId" to userId,
                    "username" to username,
                    "joinedAt" to System.currentTimeMillis()
                )

                db.collection("rooms")
                    .document(room.roomId)
                    .collection("members")
                    .document(userId)
                    .set(memberData)
                    .addOnSuccessListener {

                        db.collection("rooms")
                            .document(room.roomId)
                            .update(
                                "memberCount",
                                room.memberCount + 1
                            )

                        openRoomDashboard(room)

                    }
                    .addOnFailureListener { e ->

                        Toast.makeText(
                            requireContext(),
                            e.localizedMessage,
                            Toast.LENGTH_LONG
                        ).show()

                    }

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    requireContext(),
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()

            }
    }

    private fun openRoomDashboard(room: Room) {

        val intent = Intent(
            requireContext(),
            RoomDashboardActivity::class.java
        )

        intent.putExtra("roomId", room.roomId)
        intent.putExtra("roomName", room.roomName)
        intent.putExtra("roomDescription", room.roomDescription)

        startActivity(intent)
    }
}