package com.example.konektto.konektto.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.konektto.R
import com.example.konektto.konektto.activities.CreateRoomActivity
import com.example.konektto.konektto.fragments.HomeFragment
import com.example.konektto.konektto.fragments.SearchFragment
import com.example.konektto.konektto.fragments.ProfileFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        supportActionBar?.hide()

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Show Home when the app opens
        replaceFragment(HomeFragment())

        bottomNavigation.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }

                R.id.nav_search -> {
                    replaceFragment(SearchFragment())
                    true
                }

                R.id.nav_create -> {
                    startActivity(Intent(this, CreateRoomActivity::class.java))
                    true
                }

                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}