package com.example.konektto.konektto.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.konektto.R
import com.example.konektto.konektto.fragments.MyRoomsFragment

class MyRoomsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_rooms)

        supportActionBar?.hide()

        if (savedInstanceState == null) {

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.myRoomsContainer,
                    MyRoomsFragment()
                )
                .commit()

        }
    }
}


