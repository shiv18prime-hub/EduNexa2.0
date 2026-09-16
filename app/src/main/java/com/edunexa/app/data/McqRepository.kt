package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class McqRepository {
    private val auth=FirebaseAuth.getInstance(); private val db=FirebaseFirestore.getInstance()
    fun publishTest(title:String,subject:String,chapter:String,durationMinutes:Int,questions:List<Map<String,Any>>,onResult:(Boolean,String)->Unit){
        val schoolId=auth.currentUser?.uid?:return onResult(false,"Please login first")
        if(title.isBlank()||subject.isBlank()||questions.isEmpty())return onResult(false,"Complete test details")
        db.collection("users").document(schoolId).get().addOnSuccessListener{school->
            if(school.getString("role")!="school"||school.getBoolean("schoolApproved")!=true)return@addOnSuccessListener onResult(false,"Only approved schools can publish tests")
            val data=hashMapOf<String,Any>("schoolId" to schoolId,"title" to title.trim(),"subject" to subject.trim(),"chapter" to chapter.trim(),"durationMinutes" to durationMinutes.coerceAtLeast(1),"questions" to questions,"active" to true,"createdAt" to System.currentTimeMillis())
            db.collection("mcqTests").add(data).addOnSuccessListener{onResult(true,"MCQ test published")}.addOnFailureListener{onResult(false,it.message?:"Could not publish test")}
        }.addOnFailureListener{onResult(false,it.message?:"Could not verify school")}
    }
    fun studentTests(onResult:(List<Pair<String,Map<String,Any>>>)->Unit,onError:(String)->Unit){
        val uid=auth.currentUser?.uid?:return onError("Please login first")
        db.collection("users").document(uid).get().addOnSuccessListener{user->
            if(user.getString("role")!="student")return@addOnSuccessListener onError("Student account required")
            val schoolId=user.getString("schoolId")?:return@addOnSuccessListener onResult(emptyList())
            db.collection("mcqTests").whereEqualTo("schoolId",schoolId).whereEqualTo("active",true).get()
                .addOnSuccessListener{snap->onResult(snap.documents.mapNotNull{d->d.data?.let{d.id to it}}.sortedByDescending{(it.second["createdAt"] as? Number)?.toLong()?:0L})}
                .addOnFailureListener{onError(it.message?:"Could not load tests")}
        }.addOnFailureListener{onError(it.message?:"Could not load student profile")}
    }
    fun submit(testId:String,answers:Map<String,Int>,score:Int,total:Int,onResult:(Boolean,String)->Unit){
        val uid=auth.currentUser?.uid?:return onResult(false,"Please login first")
        val data=hashMapOf<String,Any>("testId" to testId,"studentId" to uid,"answers" to answers,"score" to score,"total" to total,"percentage" to if(total>0)score*100.0/total else 0.0,"submittedAt" to System.currentTimeMillis())
        db.collection("mcqAttempts").add(data).addOnSuccessListener{onResult(true,"Test submitted")}.addOnFailureListener{onResult(false,it.message?:"Could not submit test")}
    }
}
