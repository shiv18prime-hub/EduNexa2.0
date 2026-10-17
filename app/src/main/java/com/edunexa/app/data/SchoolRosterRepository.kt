package com.edunexa.app.data

import com.google.firebase.firestore.FirebaseFirestore

class SchoolRosterRepository {
    fun students(success: (List<Pair<String, String>>) -> Unit, failure: (String) -> Unit) {
        SchoolScope.resolve({ schoolId ->
            FirebaseFirestore.getInstance().collection("memberships")
                .whereEqualTo("schoolId", schoolId).whereEqualTo("active", true).whereEqualTo("role", "student")
                .get().addOnSuccessListener { rows ->
                    success(rows.documents.mapNotNull { row ->
                        row.getString("uid")?.let { it to (row.getString("name") ?: "Student") }
                    }.sortedBy { it.second.lowercase() })
                }.addOnFailureListener { failure(it.localizedMessage ?: "Could not load connected students") }
        }, failure)
    }
}
