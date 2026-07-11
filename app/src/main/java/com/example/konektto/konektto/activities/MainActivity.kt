package com.example.konektto.konektto.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.konektto.R
import com.example.konektto.konektto.activities.CreateRoomActivity
import com.example.konektto.konektto.fragments.HomeFragment
import com.example.konektto.konektto.fragments.SearchFragment
import com.example.konektto.konektto.fragments.ProfileFragment
import com.example.konektto.konektto.utils.NotificationHelper

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way --
            if denied, the app simply won't show local notifications; nothing else depends on it */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        supportActionBar?.hide()

        requestNotificationPermissionIfNeeded()
        NotificationHelper.refreshFcmToken()

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

    private fun requestNotificationPermissionIfNeeded() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

    }

    private fun replaceFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}