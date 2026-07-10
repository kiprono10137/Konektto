package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.konektto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var imgProfile: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvBio: TextView
    private lateinit var btnEditProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        imgProfile = findViewById(R.id.imgProfile)
        tvUsername = findViewById(R.id.tvUsername)
        tvBio = findViewById(R.id.tvBio)
        btnEditProfile = findViewById(R.id.btnEditProfile)

        loadProfile()

        btnEditProfile.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    EditProfileActivity::class.java
                )
            )

        }

    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {

        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    tvUsername.text =
                        document.getString("username")

                    tvBio.text =
                        document.getString("bio")

                    val imageUrl =
                        document.getString("profileImage")

                    if (!imageUrl.isNullOrEmpty()) {

                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .into(imgProfile)

                    }

                }

            }

    }

}


