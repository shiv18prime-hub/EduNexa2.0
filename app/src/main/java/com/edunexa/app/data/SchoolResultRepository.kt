package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SchoolResultRepository {
 private val auth=FirebaseAuth.getInstance();private val db=FirebaseFirestore.getInstance()
 fun students(onResult:(List<Pair<String,String>>)->Unit,onError:(String)->Unit){Backend.call("schoolStudents",success={r->onResult(Backend.rows(r["students"]).map{it["id"].toString() to it["name"].toString()})},failure=onError)}
 fun publish(studentId:String,studentName:String,exam:String,subject:String,obtained:Double,total:Double,onResult:(Boolean,String)->Unit){if(!obtained.isFinite()||!total.isFinite()||obtained%1.0!=0.0||total%1.0!=0.0)return onResult(false,"Enter whole-number marks");Backend.call("publishResult",mapOf("studentId" to studentId,"exam" to exam,"subject" to subject,"obtained" to obtained.toInt(),"total" to total.toInt()),{onResult(true,it["message"].toString())},{onResult(false,it)})}
 fun myResults(onResult:(List<Map<String,Any>>)->Unit,onError:(String)->Unit){val uid=auth.currentUser?.uid?:return onError("Please login first");db.collection("schoolResults").whereEqualTo("studentId",uid).whereEqualTo("published",true).get().addOnSuccessListener{snap->onResult(snap.documents.mapNotNull{it.data}.sortedByDescending{(it["createdAt"] as? Number)?.toLong()?:0L})}.addOnFailureListener{onError(it.message?:"Could not load results")}}
 fun schoolResults(onResult:(List<Map<String,Any>>)->Unit,onError:(String)->Unit){SchoolScope.resolve({sid->db.collection("schoolResults").whereEqualTo("schoolId",sid).get().addOnSuccessListener{snap->onResult(snap.documents.map{d->HashMap<String,Any>(d.data?:emptyMap()).apply{put("id",d.id)}}.sortedByDescending{(it["createdAt"] as? Number)?.toLong()?:0L})}.addOnFailureListener{onError(it.message?:"Could not load school results")}},onError)}
 fun deleteResult(id:String,onResult:(Boolean,String)->Unit){db.collection("schoolResults").document(id).delete().addOnSuccessListener{onResult(true,"Result removed")}.addOnFailureListener{onResult(false,it.message?:"Could not remove result")}}
}
