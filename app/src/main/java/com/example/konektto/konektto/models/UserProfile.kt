package com.example.konektto.konektto.models

data class UserProfile(

    val uid: String = "",
    val username: String = "",
    val bio: String = "",
    val profileImage: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L

)
