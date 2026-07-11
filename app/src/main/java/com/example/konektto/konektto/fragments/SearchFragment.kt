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
import com.example.konektto.konektto.activities.UserProfileActivity
import com.example.konektto.konektto.adapters.RoomAdapter
import com.example.konektto.konektto.adapters.UserAdapter
import com.example.konektto.konektto.models.Room
import com.example.konektto.konektto.models.UserProfile
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var etSearch: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var toggleSearchMode: MaterialButtonToggleGroup

    private lateinit var roomAdapter: RoomAdapter
    private lateinit var userAdapter: UserAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val roomList = mutableListOf<Room>()
    private val allRooms = mutableListOf<Room>()

    private val userList = mutableListOf<UserProfile>()
    private val allUsers = mutableListOf<UserProfile>()

    private var searchingUsers = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch = view.findViewById(R.id.etSearch)
        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        toggleSearchMode = view.findViewById(R.id.toggleSearchMode)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        roomAdapter = RoomAdapter(roomList) { room ->

            val intent = Intent(requireContext(), RoomDashboardActivity::class.java)
            intent.putExtra("roomId", room.roomId)
            intent.putExtra("roomName", room.roomName)
            intent.putExtra("roomDescription", room.roomDescription)

            startActivity(intent)
        }

        userAdapter = UserAdapter(userList) { user ->

            val intent = Intent(requireContext(), UserProfileActivity::class.java)
            intent.putExtra("userId", user.uid)

            startActivity(intent)
        }

        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        rvSearchResults.adapter = roomAdapter

        loadRooms()

        toggleSearchMode.check(R.id.btnSearchRooms)

        toggleSearchMode.addOnButtonCheckedListener { _, checkedId, isChecked ->

            if (!isChecked) return@addOnButtonCheckedListener

            searchingUsers = checkedId == R.id.btnSearchUsers

            if (searchingUsers) {

                rvSearchResults.adapter = userAdapter
                etSearch.hint = "Search people..."

                if (allUsers.isEmpty()) {
                    loadUsers()
                }

            } else {

                rvSearchResults.adapter = roomAdapter
                etSearch.hint = "Search communities..."

            }

            etSearch.text.clear()
        }

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
                if (searchingUsers) {
                    filterUsers(s.toString())
                } else {
                    filterRooms(s.toString())
                }
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

                roomAdapter.notifyDataSetChanged()
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

        roomAdapter.notifyDataSetChanged()
    }

    private fun loadUsers() {

        val currentUserId = auth.currentUser?.uid

        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->

                allUsers.clear()
                userList.clear()

                for (document in documents) {

                    if (document.id == currentUserId) continue

                    val user = UserProfile(
                        uid = document.id,
                        username = document.getString("username") ?: "",
                        bio = document.getString("bio") ?: "",
                        profileImage = document.getString("profileImage") ?: "",
                        isOnline = document.getBoolean("isOnline") ?: false,
                        lastSeen = document.getTimestamp("lastSeen")?.toDate()?.time ?: 0L
                    )

                    allUsers.add(user)
                    userList.add(user)
                }

                userAdapter.notifyDataSetChanged()
            }
    }

    private fun filterUsers(query: String) {

        userList.clear()

        for (user in allUsers) {

            if (user.username.contains(query, true) ||
                user.bio.contains(query, true)
            ) {
                userList.add(user)
            }
        }

        userAdapter.notifyDataSetChanged()
    }
}
