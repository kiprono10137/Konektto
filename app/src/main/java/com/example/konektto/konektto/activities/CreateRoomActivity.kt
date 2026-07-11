package com.example.konektto.konektto.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateRoomActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var etRoomName: EditText
    private lateinit var etRoomDescription: EditText
    private lateinit var spCategory: Spinner
    private lateinit var btnCreateRoom: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_room)
        supportActionBar?.hide()

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        etRoomName = findViewById(R.id.etRoomName)
        etRoomDescription = findViewById(R.id.etRoomDescription)
        spCategory = findViewById(R.id.spCategory)
        btnCreateRoom = findViewById(R.id.btnCreateRoom)

        val categories = arrayOf(
            "Football",
            "Gaming",
            "Music",
            "Programming",
            "Technology",
            "Business",
            "Fashion",
            "Movies",
            "Relationships",
            "Education",
            "General"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        spCategory.adapter = adapter

        btnCreateRoom.setOnClickListener {

            createRoom()

        }
    }

    private fun createRoom() {

        val roomName = etRoomName.text.toString().trim()
        val roomDescription = etRoomDescription.text.toString().trim()
        val category = spCategory.selectedItem.toString()

        if (roomName.isEmpty()) {
            etRoomName.error = "Room name is required"
            etRoomName.requestFocus()
            return
        }

        if (roomDescription.isEmpty()) {
            etRoomDescription.error = "Room description is required"
            etRoomDescription.requestFocus()
            return
        }

        val currentUser = auth.currentUser

        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please login again.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDocument ->

                val username =
                    userDocument.getString("username") ?: "Unknown User"

                val roomRef = db.collection("rooms").document()

                val roomData = hashMapOf(

                    "roomId" to roomRef.id,
                    "roomName" to roomName,
                    "roomDescription" to roomDescription,
                    "category" to category,
                    "creatorId" to currentUser.uid,
                    "creatorUsername" to username,
                    "createdAt" to System.currentTimeMillis(),
                    "memberCount" to 1,
                    "inviteCode" to generateInviteCode()

                )

                roomRef.set(roomData)
                    .addOnSuccessListener {

                        // Add the creator as the first member
                        val memberData = hashMapOf(
                            "userId" to currentUser.uid,
                            "username" to username,
                            "joinedAt" to System.currentTimeMillis(),
                            "role" to "owner"
                        )

                        roomRef.collection("members")
                            .document(currentUser.uid)
                            .set(memberData)
                            .addOnSuccessListener {

                                Toast.makeText(
                                    this,
                                    "Community created successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                finish()

                            }

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

    private fun generateInviteCode(): String {

        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I -- easy to misread aloud
        return (1..6).map { chars.random() }.joinToString("")

    }
}