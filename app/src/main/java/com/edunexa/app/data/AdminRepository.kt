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

    fun setSchoolApproval(uid:String,approved:Boolean,onResult:(Boolean,String)->Unit){Backend.call("rotateCode",mapOf("schoolId" to uid,"approved" to approved),{onResult(true,"${it["message"]} • ${it["code"]}")},{onResult(false,it)})}
}
