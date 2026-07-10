package com.example.konektto.konektto.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.konektto.R


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        supportActionBar?.hide()

        setContentView(R.layout.activity_splash)

                Handler(Looper.getMainLooper()).postDelayed({

                    val intent = Intent(/* packageContext = */ this, /* cls = */ LoginActivity::class.java)
                    startActivity(intent)

            finish()

        }, 4000)
    }
}
