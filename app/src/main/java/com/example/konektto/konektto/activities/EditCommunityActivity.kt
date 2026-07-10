package com.example.konektto.konektto.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.R
import com.google.firebase.firestore.FirebaseFirestore

class EditCommunityActivity : AppCompatActivity() {

    private lateinit var etRoomName: EditText
    private lateinit var etDescription: EditText
    private lateinit var etCategory: EditText
    private lateinit var btnSave: Button

    private lateinit var db: FirebaseFirestore

    private lateinit var roomId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_community)
        supportActionBar?.hide()

        etRoomName = findViewById(R.id.etRoomName)
        etDescription = findViewById(R.id.etDescription)
        etCategory = findViewById(R.id.etCategory)
        btnSave = findViewById(R.id.btnSave)

        db = FirebaseFirestore.getInstance()

        roomId = intent.getStringExtra("roomId") ?: ""

        loadCommunity()

        btnSave.setOnClickListener {
            saveCommunity()
        }
    }

    private fun loadCommunity() {

        db.collection("rooms")
            .document(roomId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) return@addOnSuccessListener

                etRoomName.setText(
                    document.getString("roomName")
                )

                etDescription.setText(
                    document.getString("roomDescription")
                )

                etCategory.setText(
                    document.getString("category")
                )

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()

            }

    }

    private fun saveCommunity() {

        val roomName = etRoomName.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val category = etCategory.text.toString().trim()

        if (roomName.isEmpty()) {
            etRoomName.error = "Required"
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "Required"
            return
        }

        if (category.isEmpty()) {
            etCategory.error = "Required"
            return
        }

        val updates = hashMapOf<String, Any>(
            "roomName" to roomName,
            "roomDescription" to description,
            "category" to category
        )

        db.collection("rooms")
            .document(roomId)
            .update(updates)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Community updated successfully!",
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