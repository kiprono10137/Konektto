package com.example.konektto.konektto.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.konektto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore



class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

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
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (username.isEmpty()) {
                etUsername.error = "Username is required"
                etUsername.requestFocus()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                etEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Password is required"
                etPassword.requestFocus()
                return@setOnClickListener

            }

            if (confirmPassword.isEmpty()) {
                etConfirmPassword.error = "Confirm your password"
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        val user = auth.currentUser
                        val userData = hashMapOf(
                            "username" to username,
                            "email" to email,
                            "bio" to "",
                            "roomsCreated" to 0,
                            "roomsJoined" to 0,
                            "reputation" to 0
                        )
                        db.collection("users")
                            .document(user!!.uid)
                            .set(userData)
                            .addOnSuccessListener {

                                user.sendEmailVerification()
                                    .addOnCompleteListener {

                                        Toast.makeText(
                                            this,
                                            "Account created! Please verify your email before logging in.",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        auth.signOut()

                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()

                                    }

                            }
                            .addOnFailureListener { e ->

                                Toast.makeText(
                                    this,
                                    "Failed to save profile: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                            }

                        user?.sendEmailVerification()
                            ?.addOnCompleteListener {

                                Toast.makeText(
                                    this,
                                    "Account created! Please verify your email before logging in.",
                                    Toast.LENGTH_LONG
                                ).show()

                                auth.signOut()

                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }

                    } else {

                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Registration failed",
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }





        }
    }
}
