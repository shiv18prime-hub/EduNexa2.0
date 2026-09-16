package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NoticeRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun publish(title: String, message: String, targetClass: String, important: Boolean, onResult: (Boolean, String) -> Unit) {
        val schoolId = auth.currentUser?.uid ?: return onResult(false, "Please login first")
        if (title.isBlank() || message.isBlank()) return onResult(false, "Title and message are required")
        db.collection("users").document(schoolId).get().addOnSuccessListener { school ->
            if (school.getString("role") != "school" || school.getBoolean("schoolApproved") != true)
                return@addOnSuccessListener onResult(false, "Only approved schools can publish notices")
            val data = hashMapOf(
                "schoolId" to schoolId,
                "title" to title.trim(),
                "message" to message.trim(),
                "targetClass" to targetClass.trim(),
                "important" to important,
                "active" to true,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("notices").add(data)
                .addOnSuccessListener { onResult(true, "Notice published") }
                .addOnFailureListener { onResult(false, it.message ?: "Could not publish notice") }
        }.addOnFailureListener { onResult(false, it.message ?: "Could not verify school") }
    }

    fun studentNotices(onResult: (List<Map<String, Any>>) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onError("Please login first")
        db.collection("users").document(uid).get().addOnSuccessListener { user ->
            val schoolId = user.getString("schoolId") ?: return@addOnSuccessListener onResult(emptyList())
            db.collection("notices").whereEqualTo("schoolId", schoolId).whereEqualTo("active", true)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get()
                .addOnSuccessListener { snap -> onResult(snap.documents.mapNotNull { it.data }) }
                .addOnFailureListener { onError(it.message ?: "Could not load notices") }
        }.addOnFailureListener { onError(it.message ?: "Could not load student profile") }
    }
}
