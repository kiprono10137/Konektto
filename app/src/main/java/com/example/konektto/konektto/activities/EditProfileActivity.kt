package com.example.konektto.konektto.activities

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.konektto.R
import com.example.konektto.konektto.utils.CloudinaryUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var imgProfile: ImageView
    private lateinit var etUsername: EditText
    private lateinit var etBio: EditText
    private lateinit var btnChooseImage: Button
    private lateinit var btnSave: Button

    private var selectedImageUri: Uri? = null

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                imgProfile.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        imgProfile = findViewById(R.id.imgProfile)
        etUsername = findViewById(R.id.etUsername)
        etBio = findViewById(R.id.etBio)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        btnSave = findViewById(R.id.btnSave)

        loadProfile()

        btnChooseImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        btnSave.setOnClickListener {

            if (selectedImageUri != null) {
                uploadProfilePicture()
            } else {
                saveProfile(null)
            }

        }
    }

    private fun loadProfile() {

        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    etUsername.setText(document.getString("username"))
                    etBio.setText(document.getString("bio"))

                    val imageUrl = document.getString("profileImage")

                    if (!imageUrl.isNullOrBlank()) {

                        Glide.with(this)
                            .load(imageUrl)
                            .circleCrop()
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .error(android.R.drawable.sym_def_app_icon)
                            .into(imgProfile)

                    }

                }

            }

    }

    private fun saveProfile(imageUrl: String?) {

        val currentUser = auth.currentUser ?: return

        val userData = hashMapOf<String, Any>(
            "username" to etUsername.text.toString().trim(),
            "bio" to etBio.text.toString().trim()
        )

        if (imageUrl != null) {
            userData["profileImage"] = imageUrl
        }

        db.collection("users")
            .document(currentUser.uid)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Profile updated successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    e.localizedMessage ?: "Failed to save profile",
                    Toast.LENGTH_LONG
                ).show()

            }

    }

    private fun uploadProfilePicture() {

        val uri = selectedImageUri ?: return

        try {

            val bitmap =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                    val source =
                        ImageDecoder.createSource(contentResolver, uri)

                    ImageDecoder.decodeBitmap(source)

                } else {

                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)

                }

            val outputStream = ByteArrayOutputStream()

            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                80,
                outputStream
            )

            val imageBytes = outputStream.toByteArray()

            CloudinaryUploader.uploadImage(imageBytes) { imageUrl ->

                runOnUiThread {

                    if (imageUrl != null) {

                        saveProfile(imageUrl)

                    } else {

                        Toast.makeText(
                            this,
                            "Image upload failed.",
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }

            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                e.localizedMessage,
                Toast.LENGTH_LONG
            ).show()

        }

    }

}