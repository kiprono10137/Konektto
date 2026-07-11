package com.example.konektto.konektto.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.konektto.R
import com.example.konektto.konektto.activities.EditProfileActivity
import com.example.konektto.konektto.activities.FriendRequestsActivity
import com.example.konektto.konektto.activities.LoginActivity
import com.example.konektto.konektto.activities.MyRoomsActivity
import com.example.konektto.konektto.activities.SettingsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var imgProfile: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        imgProfile = view.findViewById(R.id.imgProfile)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvEmail = view.findViewById(R.id.tvEmail)

        val btnChangePhoto = view.findViewById<Button>(R.id.btnChangePhoto)
        val btnEditProfile = view.findViewById<Button>(R.id.btnEditProfile)
        val btnMyRooms = view.findViewById<Button>(R.id.btnMyRooms)
        val btnFriendRequests = view.findViewById<Button>(R.id.btnFriendRequests)
        val btnSettings = view.findViewById<Button>(R.id.btnSettings)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        loadProfile()

        btnChangePhoto.setOnClickListener {
            startActivity(
                Intent(requireContext(), EditProfileActivity::class.java)
            )
        }

        btnFriendRequests.setOnClickListener {
            startActivity(
                Intent(requireContext(), FriendRequestsActivity::class.java)
            )
        }

        btnEditProfile.setOnClickListener {
            startActivity(
                Intent(requireContext(), EditProfileActivity::class.java)
            )
        }

        btnMyRooms.setOnClickListener {
            startActivity(
                Intent(requireContext(), MyRoomsActivity::class.java)
            )
        }

        btnSettings.setOnClickListener {
            startActivity(
                Intent(requireContext(), SettingsActivity::class.java)
            )
        }

        btnLogout.setOnClickListener {

            com.example.konektto.konektto.utils.PresenceManager.setOfflineImmediately()

            auth.signOut()

            val intent = Intent(
                requireContext(),
                LoginActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {

        val currentUser = auth.currentUser ?: return

        tvEmail.text = currentUser.email

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    Log.e("PROFILE", "User document does not exist.")
                    return@addOnSuccessListener
                }

                val username = document.getString("username")
                val imageUrl = document.getString("profileImage")?.trim()

                tvUsername.text = username ?: "Unknown User"

                Log.d("PROFILE", "Username = $username")
                Log.d("PROFILE", "Firestore URL = '$imageUrl'")

                if (!imageUrl.isNullOrEmpty()) {

                    Glide.with(requireActivity())
                        .load(imageUrl)
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .error(android.R.drawable.sym_def_app_icon)
                        .circleCrop()
                        .into(imgProfile)

                } else {

                    imgProfile.setImageResource(
                        android.R.drawable.sym_def_app_icon
                    )

                }

            }
            .addOnFailureListener { e ->

                Log.e("PROFILE", "Firestore Error", e)

            }

    }

}