package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SchoolScope {
    fun resolve(success: (String) -> Unit, failure: (String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return failure("Please sign in")
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).get().addOnSuccessListener profileLoaded@ { profile ->
            val isSchool = profile.getString("role") == "school" && profile.getBoolean("schoolApproved") == true
            val isTeacher = profile.getString("role") == "teacher" && profile.getString("schoolJoinStatus") == "connected"
            val sid = if (isSchool) uid else if (isTeacher) profile.getString("schoolId").orEmpty() else ""
            if (sid.isBlank()) { failure("Connect to an approved school first"); return@profileLoaded }
            db.collection("schoolApprovals").document(sid).get().addOnSuccessListener approvalLoaded@ { approval ->
                if (approval.getBoolean("approved") != true) { failure("Your school needs administrator approval"); return@approvalLoaded }
                if (isSchool) success(sid) else db.collection("memberships").document("${sid}_$uid").get()
                    .addOnSuccessListener { membership ->
                        if (membership.getBoolean("active") == true && membership.getString("role") == "teacher") success(sid)
                        else failure("Your school connection is no longer active")
                    }.addOnFailureListener { failure(it.localizedMessage ?: "Could not check school membership") }
            }.addOnFailureListener { failure(it.localizedMessage ?: "Could not check school approval") }
        }.addOnFailureListener { failure(it.localizedMessage ?: "Could not load school") }
    }
}
