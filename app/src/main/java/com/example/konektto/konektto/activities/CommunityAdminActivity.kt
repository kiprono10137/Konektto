package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.R
import com.google.firebase.firestore.FirebaseFirestore

class CommunityAdminActivity : AppCompatActivity() {

    private lateinit var btnEditRoom: Button
    private lateinit var btnDeleteRoom: Button
    private lateinit var btnAnnouncements: Button

    private lateinit var roomId: String
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_admin)
        supportActionBar?.hide()

        db = FirebaseFirestore.getInstance()

        roomId = intent.getStringExtra("roomId") ?: ""

        btnEditRoom = findViewById(R.id.btnEditRoom)
        btnDeleteRoom = findViewById(R.id.btnDeleteRoom)
        btnAnnouncements = findViewById(R.id.btnAnnouncements)

        // Edit Community
        btnEditRoom.setOnClickListener {

            val intent = Intent(
                this,
                EditCommunityActivity::class.java
            )

            intent.putExtra("roomId", roomId)

            startActivity(intent)

        }

        // Delete Community
        btnDeleteRoom.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Delete Community")
                .setMessage("Are you sure you want to permanently delete this community? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->

                    deleteCommunity()

                }
                .setNegativeButton("Cancel", null)
                .show()

        }

        // Community Announcements
        btnAnnouncements.setOnClickListener {

            val intent = Intent(
                this,
                AnnouncementActivity::class.java
            )

            intent.putExtra("roomId", roomId)

            startActivity(intent)

        }

    }

    private fun deleteCommunity() {

        db.collection("rooms")
            .document(roomId)
            .delete()
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Community deleted successfully!",
                    Toast.LENGTH_LONG
                ).show()

                finish()

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