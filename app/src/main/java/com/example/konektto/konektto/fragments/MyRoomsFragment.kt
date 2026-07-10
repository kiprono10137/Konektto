package com.example.konektto.konektto.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
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

class MyRoomsFragment : Fragment(R.layout.fragment_my_rooms) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RoomAdapter

    private lateinit var db: FirebaseFirestore

    private val roomList = mutableListOf<Room>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvMyRooms)

        db = FirebaseFirestore.getInstance()

        adapter = RoomAdapter(roomList) { room ->

            val intent = Intent(
                requireContext(),
                RoomDashboardActivity::class.java
            )

            intent.putExtra("roomId", room.roomId)
            intent.putExtra("roomName", room.roomName)

            startActivity(intent)

        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadMyRooms()
    }

    private fun loadMyRooms() {

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        db.collection("rooms")
            .get()
            .addOnSuccessListener { documents ->

                roomList.clear()

                var roomsChecked = 0

                if (documents.isEmpty) {
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                for (document in documents) {

                    val room = document.toObject(Room::class.java)

                    db.collection("rooms")
                        .document(room.roomId)
                        .collection("members")
                        .document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { memberDoc ->

                            if (memberDoc.exists()) {
                                roomList.add(room)
                            }

                            roomsChecked++

                            if (roomsChecked == documents.size()) {
                                adapter.notifyDataSetChanged()
                            }

                        }

                }

            }
            .addOnFailureListener {

                Toast.makeText(
                    requireContext(),
                    "Failed to load your rooms.",
                    Toast.LENGTH_SHORT
                ).show()

            }

    }
}