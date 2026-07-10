package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {

            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (username.isEmpty()) {
                etUsername.error = "Username is required"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Password is required"
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                etConfirmPassword.error = "Confirm your password"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {

                    val user = auth.currentUser!!

                    val userData = hashMapOf(
                        "username" to username,
                        "email" to email,
                        "bio" to "",
                        "roomsCreated" to 0,
                        "roomsJoined" to 0,
                        "reputation" to 0
                    )

                    db.collection("users")
                        .document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {

                            Log.d("Firestore", "User document created")

                            user.sendEmailVerification()

                            Toast.makeText(
                                this,
                                "Account created! Verify your email before logging in.",
                                Toast.LENGTH_LONG
                            ).show()

                            auth.signOut()

                            startActivity(
                                Intent(this, LoginActivity::class.java)
                            )

                            finish()

                        }
                        .addOnFailureListener { e ->

                            Log.e("Firestore", "Firestore Error", e)

                            Toast.makeText(
                                this,
                                "Firestore Error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()

                        }

                }
                .addOnFailureListener { e ->

                    Toast.makeText(
                        this,
                        e.message,
                        Toast.LENGTH_LONG
                    ).show()

                }

        }

    }
}
