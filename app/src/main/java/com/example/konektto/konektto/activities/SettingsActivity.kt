package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.R
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        val btnAccount = findViewById<Button>(R.id.btnAccount)
        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)
        val btnAbout = findViewById<Button>(R.id.btnAbout)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnAccount.setOnClickListener {

            Toast.makeText(
                this,
                "Account information coming soon.",
                Toast.LENGTH_SHORT
            ).show()

        }

        btnResetPassword.setOnClickListener {

            val email = auth.currentUser?.email

            if (email != null) {

                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Password reset email sent.",
                            Toast.LENGTH_LONG
                        ).show()

                    }
                    .addOnFailureListener {

                        Toast.makeText(
                            this,
                            "Failed to send email.",
                            Toast.LENGTH_LONG
                        ).show()

                    }

            }

        }

        btnAbout.setOnClickListener {

            Toast.makeText(
                this,
                "Konektto\nVersion 1.0",
                Toast.LENGTH_LONG
            ).show()

        }

        btnLogout.setOnClickListener {

            auth.signOut()

            val intent = Intent(
                this,
                LoginActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

        }

    }
}