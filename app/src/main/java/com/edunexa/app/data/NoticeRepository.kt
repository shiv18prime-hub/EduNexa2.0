package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NoticeRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun publish(title:String,message:String,targetClass:String,important:Boolean,onResult:(Boolean,String)->Unit){
        val uid=auth.currentUser?.uid?:return onResult(false,"Please sign in first")
        SchoolScope.resolve({sid->
            val record=try{com.edunexa.app.core.SchoolRecords.notice(sid,uid,title,message,targetClass,important,System.currentTimeMillis())}
                catch(e:IllegalArgumentException){onResult(false,e.message?:"Check the notice details");return@resolve}
            db.collection("notices").add(record).addOnSuccessListener{onResult(true,"Notice published")}
                .addOnFailureListener{onResult(false,it.localizedMessage?:"Could not publish notice")}
        },{onResult(false,it)})
    }

    fun studentNotices(onResult: (List<Map<String, Any>>) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onError("Please login first")
        db.collection("users").document(uid).get().addOnSuccessListener { user ->
            val schoolId = user.getString("schoolId") ?: return@addOnSuccessListener onResult(emptyList())
            db.collection("notices").whereEqualTo("schoolId", schoolId)
                .get()
                .addOnSuccessListener { snap -> onResult(snap.documents.mapNotNull { it.data }.filter { row -> val target=row["targetClass"]?.toString().orEmpty(); target.isBlank() || target==user.getString("className") }.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0 }.take(50)) }
                .addOnFailureListener { onError(it.message ?: "Could not load notices") }
        }.addOnFailureListener { onError(it.message ?: "Could not load student profile") }
    }
}
