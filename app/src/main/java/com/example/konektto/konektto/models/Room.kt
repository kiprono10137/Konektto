package com.example.konektto.konektto.models

data class Room(
    val roomId: String = "",
    val roomName: String = "",
    val roomDescription: String = "",
    val category: String = "",
    val ageGroup: String = "",
    val gender: String = "",
    val creatorId: String = "",
    val creatorUsername: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val memberCount: Int = 1,

    val pinnedMessageId: String = "",
    val pinnedMessageText: String = "",
    val pinnedMessageSenderName: String = ""
)