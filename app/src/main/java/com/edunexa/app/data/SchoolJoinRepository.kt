package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SchoolJoinRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun joinSchool(code: String, onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(false, "Please login first")
        val normalized = code.trim().uppercase()
        if (normalized.isBlank()) return onResult(false, "Enter school code")
        db.collection("schoolCodes").document(normalized).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists() || doc.getBoolean("active") != true) return@addOnSuccessListener onResult(false, "Invalid school code")
                val schoolId = doc.getString("schoolId") ?: return@addOnSuccessListener onResult(false, "Invalid school record")
                db.collection("users").document(uid).update("schoolId", schoolId, "schoolCode", normalized)
                    .addOnSuccessListener { onResult(true, "Connected to verified school") }
                    .addOnFailureListener { onResult(false, it.message ?: "Could not join school") }
            }
            .addOnFailureListener { onResult(false, it.message ?: "Could not verify school code") }
    }
}
