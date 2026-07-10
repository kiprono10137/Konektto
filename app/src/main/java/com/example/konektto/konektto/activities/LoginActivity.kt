package com.example.konektto.konektto.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.R
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth


class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.hide()
        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)


        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

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


            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        val user = auth.currentUser

                        if (user != null && user.isEmailVerified) {

                            Toast.makeText(
                                this,
                                "Login successful!",
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()

                        } else {

                            auth.signOut()

                            Toast.makeText(
                                this,
                                "Please verify your email before logging in.",
                                Toast.LENGTH_LONG
                            ).show()

                        }

                    } else {

                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Login failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }


                }
        }
    }
}