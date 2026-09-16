package com.edunexa.app.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class AdminRepository {
    private val db = FirebaseFirestore.getInstance()

    fun pendingSchools(onResult: (List<UserProfile>) -> Unit, onError: (String) -> Unit) {
        db.collection("users").whereEqualTo("role", "school").whereEqualTo("schoolApproved", false).get()
            .addOnSuccessListener { snap -> onResult(snap.toObjects(UserProfile::class.java)) }
            .addOnFailureListener { onError(it.message ?: "Could not load schools") }
    }

    fun setSchoolApproval(uid: String, approved: Boolean, onResult: (Boolean, String) -> Unit) {
        if (!approved) {
            db.collection("users").document(uid).update("schoolApproved", false, "approvalStatus", "rejected")
                .addOnSuccessListener { onResult(true, "School rejected") }
                .addOnFailureListener { onResult(false, it.message ?: "Update failed") }
            return
        }
        val code = "EDU-${Random.nextInt(100000, 999999)}"
        val userRef = db.collection("users").document(uid)
        val codeRef = db.collection("schoolCodes").document(code)
        db.runBatch { batch ->
            batch.update(userRef, mapOf("schoolApproved" to true, "approvalStatus" to "approved", "schoolCode" to code))
            batch.set(codeRef, mapOf("code" to code, "schoolId" to uid, "active" to true, "createdAt" to System.currentTimeMillis()))
        }.addOnSuccessListener { onResult(true, "School approved • Code: $code") }
            .addOnFailureListener { onResult(false, it.message ?: "Approval failed") }
    }
}
