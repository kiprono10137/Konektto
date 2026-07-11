package com.example.konektto.konektto.models

data class PrivateMessage(

    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val read: Boolean = false,

    // "" (text-only), "image", "gif", "audio", or "file"
    val attachmentType: String = "",
    val attachmentUrl: String = "",
    val attachmentName: String = "",     // original filename, for "file"
    val attachmentSize: Long = 0L,       // bytes, for "file"
    val attachmentDuration: Long = 0L    // milliseconds, for "audio"

)