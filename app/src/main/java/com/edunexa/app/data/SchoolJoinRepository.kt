package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class SchoolJoinRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun joinByCode(code: String, onResult: (Boolean, String) -> Unit) = joinSchool(code, onResult)

    fun joinSchool(code: String, onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(false, "Please login first")
        val normalized = code.trim().uppercase()
        if (normalized.isBlank()) return onResult(false, "Enter school code")
        db.collection("schoolCodes").document(normalized).get().addOnSuccessListener { codeDoc ->
            if (!codeDoc.exists() || codeDoc.getBoolean("active") != true) return@addOnSuccessListener onResult(false, "Invalid school code")
            val schoolId = codeDoc.getString("schoolId") ?: return@addOnSuccessListener onResult(false, "Invalid school record")
            db.collection("users").document(schoolId).get().addOnSuccessListener { school ->
                if (school.getString("role") != "school" || school.getBoolean("schoolApproved") != true) return@addOnSuccessListener onResult(false, "School is not verified")
                db.collection("users").document(uid).get().addOnSuccessListener { user ->
                    if (user.getString("role") != "student") return@addOnSuccessListener onResult(false, "Only student accounts can join a school")
                    if (user.getString("schoolId") == schoolId && user.getString("schoolJoinStatus") == "connected") return@addOnSuccessListener onResult(true, "Already connected to this school")
                    val requestId = "${schoolId}_$uid"
                    val request = hashMapOf<String, Any>(
                        "schoolId" to schoolId, "schoolName" to (school.getString("schoolName") ?: school.getString("name") ?: "School"),
                        "studentId" to uid, "studentName" to (user.getString("name") ?: "Student"), "studentEmail" to (user.getString("email") ?: ""),
                        "className" to (user.getString("className") ?: ""), "schoolCode" to normalized, "status" to "pending",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("schoolJoinRequests").document(requestId).set(request).addOnSuccessListener {
                        db.collection("users").document(uid).update("schoolJoinStatus", "pending", "pendingSchoolId", schoolId, "pendingSchoolCode", normalized)
                            .addOnSuccessListener { onResult(true, "Connection request sent. Your school must approve it.") }
                            .addOnFailureListener { onResult(false, it.message ?: "Could not update request status") }
                    }.addOnFailureListener { onResult(false, it.message ?: "Could not send school request") }
                }.addOnFailureListener { onResult(false, it.message ?: "Could not verify student") }
            }.addOnFailureListener { onResult(false, it.message ?: "Could not verify school") }
        }.addOnFailureListener { onResult(false, it.message ?: "Could not verify school code") }
    }
}
