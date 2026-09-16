package com.edunexa.app.data

import com.google.firebase.firestore.FirebaseFirestore

class AdminRepository {
    private val db = FirebaseFirestore.getInstance()

    fun pendingSchools(onResult: (List<UserProfile>) -> Unit, onError: (String) -> Unit) {
        db.collection("users")
            .whereEqualTo("role", "school")
            .whereEqualTo("schoolApproved", false)
            .get()
            .addOnSuccessListener { snap -> onResult(snap.toObjects(UserProfile::class.java)) }
            .addOnFailureListener { onError(it.message ?: "Could not load schools") }
    }

    fun setSchoolApproval(uid: String, approved: Boolean, onResult: (Boolean, String) -> Unit) {
        db.collection("users").document(uid)
            .update("schoolApproved", approved, "approvalStatus", if (approved) "approved" else "rejected")
            .addOnSuccessListener { onResult(true, if (approved) "School approved" else "School rejected") }
            .addOnFailureListener { onResult(false, it.message ?: "Update failed") }
    }
}
