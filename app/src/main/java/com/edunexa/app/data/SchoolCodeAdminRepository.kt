package com.edunexa.app.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class SchoolCodeAdminRepository {
    private val db = FirebaseFirestore.getInstance()

    fun createForSchool(schoolId: String, onResult: (Boolean, String) -> Unit) {
        if (schoolId.isBlank()) return onResult(false, "Invalid school")
        val code = "EDU-${Random.nextInt(100000, 999999)}"
        val data = hashMapOf(
            "code" to code,
            "schoolId" to schoolId,
            "active" to true,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("schoolCodes").document(code).set(data)
            .addOnSuccessListener { onResult(true, code) }
            .addOnFailureListener { onResult(false, it.message ?: "Could not create school code") }
    }
}
