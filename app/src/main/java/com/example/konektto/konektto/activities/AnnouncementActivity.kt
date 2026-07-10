package com.example.konektto.konektto.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.R
import com.google.firebase.firestore.FirebaseFirestore

class AnnouncementActivity : AppCompatActivity() {

    private lateinit var etAnnouncement: EditText
    private lateinit var btnPostAnnouncement: Button

    private lateinit var db: FirebaseFirestore
    private lateinit var roomId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_announcement)

        supportActionBar?.hide()
        etAnnouncement = findViewById(R.id.etAnnouncement)
        btnPostAnnouncement = findViewById(R.id.btnPostAnnouncement)

        db = FirebaseFirestore.getInstance()

        roomId = intent.getStringExtra("roomId") ?: ""

        loadAnnouncement()

        btnPostAnnouncement.setOnClickListener {
            postAnnouncement()
        }
    }

    private fun loadAnnouncement() {

        db.collection("rooms")
            .document(roomId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    etAnnouncement.setText(
                        document.getString("announcement") ?: ""
                    )

                }

            }

    }

    private fun postAnnouncement() {

        val announcement =
            etAnnouncement.text.toString().trim()

        if (announcement.isEmpty()) {

            etAnnouncement.error = "Enter an announcement"

            return

        }

        db.collection("rooms")
            .document(roomId)
            .update(
                "announcement",
                announcement
            )
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Announcement posted!",
                    Toast.LENGTH_SHORT
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

