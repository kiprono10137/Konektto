package com.example.konektto.konektto.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.konektto.R
import com.example.konektto.konektto.activities.RoomDashboardActivity
import com.example.konektto.konektto.adapters.RoomAdapter
import com.example.konektto.konektto.models.Room
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var etSearch: EditText
    private lateinit var rvSearchResults: RecyclerView

    private lateinit var adapter: RoomAdapter
    private lateinit var db: FirebaseFirestore

    private val roomList = mutableListOf<Room>()
    private val allRooms = mutableListOf<Room>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch = view.findViewById(R.id.etSearch)
        rvSearchResults = view.findViewById(R.id.rvSearchResults)

        db = FirebaseFirestore.getInstance()

        adapter = RoomAdapter(roomList) { room ->

            val intent = Intent(requireContext(), RoomDashboardActivity::class.java)
            intent.putExtra("roomId", room.roomId)
            intent.putExtra("roomName", room.roomName)
            intent.putExtra("roomDescription", room.roomDescription)

            startActivity(intent)
        }

        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        rvSearchResults.adapter = adapter

        loadRooms()

        etSearch.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                filterRooms(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadRooms() {

        db.collection("rooms")
            .get()
            .addOnSuccessListener { documents ->

                allRooms.clear()
                roomList.clear()

                for (document in documents) {

                    val room = document.toObject(Room::class.java)

                    allRooms.add(room)
                    roomList.add(room)
                }

                adapter.notifyDataSetChanged()
            }
    }

    private fun filterRooms(query: String) {

        roomList.clear()

        for (room in allRooms) {

            if (room.roomName.contains(query, true) ||
                room.roomDescription.contains(query, true)
            ) {
                roomList.add(room)
            }
        }

        adapter.notifyDataSetChanged()
    }
}