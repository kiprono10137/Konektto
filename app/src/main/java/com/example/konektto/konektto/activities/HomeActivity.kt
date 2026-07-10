package com.example.konektto.konektto.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.adapters.RoomAdapter
import com.example.konektto.konektto.models.Room
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RoomAdapter
    private lateinit var db: FirebaseFirestore

    private val roomList = mutableListOf<Room>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        supportActionBar?.hide()

        recyclerView = findViewById(R.id.rvRooms)

        db = FirebaseFirestore.getInstance()

        adapter = RoomAdapter(roomList) { room ->
            // We'll open the room later
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadRooms()
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
    }
}