package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ResultRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun save(obtained: Double, total: Double, percentage: Double, grade: String, onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(false, "Please login first")
        val data = hashMapOf(
            "userId" to uid,
            "obtained" to obtained,
            "total" to total,
            "percentage" to percentage,
            "grade" to grade,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("results").add(data)
            .addOnSuccessListener { onResult(true, "Result saved") }
            .addOnFailureListener { onResult(false, it.message ?: "Could not save result") }
    }

    fun history(onResult: (List<Map<String, Any>>) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onError("Please login first")
        db.collection("results").whereEqualTo("userId", uid).orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get()
            .addOnSuccessListener { snap -> onResult(snap.documents.mapNotNull { it.data }) }
            .addOnFailureListener { onError(it.message ?: "Could not load result history") }
    }
}
