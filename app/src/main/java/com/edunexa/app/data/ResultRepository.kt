package com.edunexa.app.data
import com.edunexa.app.core.SubjectMarks
import com.edunexa.app.core.MarksCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ResultRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    fun saveSubjects(exam: String, subjects: List<SubjectMarks>, onResult: (Boolean, String) -> Unit) {
        val uid=auth.currentUser?.uid?:return onResult(false,"Please login first")
        val calculated=try{MarksCalculator.calculate(subjects)}catch(e:IllegalArgumentException){return onResult(false,e.message?:"Enter valid marks")}
        val record=mapOf("userId" to uid,"personal" to true,"exam" to exam.trim().take(120).ifBlank{"My result"},
            "subjects" to subjects.map{it.asMap()},"total" to calculated.total,"obtained" to calculated.obtained,
            "percentage" to calculated.percentage,"grade" to calculated.grade,
            "result" to if(calculated.passed)"PASS" else "FAIL","createdAt" to System.currentTimeMillis())
        db.collection("results").add(record).addOnSuccessListener{onResult(true,"Result saved and synced")}
            .addOnFailureListener{onResult(false,it.message?:"Could not save result")}
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
