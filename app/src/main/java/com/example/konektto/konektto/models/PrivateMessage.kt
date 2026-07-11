package com.example.konektto.konektto.models

data class PrivateMessage(

    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val read: Boolean = false

)