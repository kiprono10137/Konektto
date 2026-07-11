package com.example.konektto.konektto.models

data class Message(

    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,

    val attachmentType: String = "",
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val attachmentSize: Long = 0L,
    val attachmentDuration: Long = 0L

)