package com.example.konektto.konektto.models

data class FriendRequest(

    val requestId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val status: String = "", // "pending" or "accepted"
    val timestamp: Long = 0L

)
