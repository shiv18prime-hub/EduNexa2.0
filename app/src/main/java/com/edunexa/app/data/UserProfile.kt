package com.edunexa.app.data

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "student",
    val schoolName: String = "",
    val schoolApproved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
