package com.example.konektto.konektto.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateRoomActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_room)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val etRoomName = findViewById<EditText>(R.id.etRoomName)
        val etRoomDescription = findViewById<EditText>(R.id.etRoomDescription)
        val btnCreateRoom = findViewById<Button>(R.id.btnCreateRoom)

        btnCreateRoom.setOnClickListener {

            Toast.makeText(this, "1. Button clicked", Toast.LENGTH_SHORT).show()

            val roomName = etRoomName.text.toString().trim()
            val roomDescription = etRoomDescription.text.toString().trim()

            if (roomName.isEmpty()) {
                etRoomName.error = "Room name is required"
etRoomName.requestFocus()
                return@setOnClickListener
            }

            if (roomDescription.isEmpty()) {
                etRoomDescription.error = "Room description is required"
                etRoomDescription.requestFocus()
                return@setOnClickListener
            }

            Toast.makeText(this, "2. Inputs OK", Toast.LENGTH_SHORT).show()

            val currentUser = auth.currentUser

            if (currentUser == null) {
                Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "3. User Found", Toast.LENGTH_SHORT).show()

            val roomRef = db.collection("rooms").document()

            val roomData = hashMapOf(
                "roomId" to roomRef.id,
                "roomName" to roomName,
                "roomDescription" to roomDescription,
                "creatorId" to currentUser.uid,
                "createdAt" to System.currentTimeMillis(),
                "memberCount" to 1
            )

            Toast.makeText(this, "4. Saving...", Toast.LENGTH_SHORT).show()

            roomRef.set(roomData)
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Room created successfully!",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }
                .addOnFailureListener { e ->

                    Toast.makeText(
                        this,
                        "Firestore Error:\n${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()

                    e.printStackTrace()
                }
        }
    }
}