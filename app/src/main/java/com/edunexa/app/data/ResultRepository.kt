package com.edunexa.app.data
import com.edunexa.app.core.SubjectMarks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ResultRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    fun saveSubjects(exam: String, subjects: List<SubjectMarks>, onResult: (Boolean, String) -> Unit) {
        Backend.call("saveResult", mapOf("exam" to exam, "subjects" to subjects.map { it.asMap() }), { onResult(true, "Result saved and synced") }, { onResult(false, it) })
    }
    fun history(onResult: (List<Map<String, Any>>) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onError("Please login first")
        db.collection("results").whereEqualTo("userId", uid).get().addOnSuccessListener { snap ->
            onResult(snap.documents.mapNotNull { it.data?.toMutableMap()?.apply { put("id", it.id) } }.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0 }.take(50))
        }.addOnFailureListener { onError(it.message ?: "Could not load result history") }
    }
    fun delete(id: String, onResult: (Boolean, String) -> Unit) {
        db.collection("results").document(id).delete().addOnSuccessListener { onResult(true, "Result removed") }.addOnFailureListener { onResult(false, it.message ?: "Could not remove result") }
    }
}
