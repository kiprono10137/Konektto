package com.example.konektto.konektto.models

data class Member(

    val userId: String = "",
    val username: String = "",
    val joinedAt: Long = 0L,
    val role: String = "member" // "owner", "moderator", or "member"

)