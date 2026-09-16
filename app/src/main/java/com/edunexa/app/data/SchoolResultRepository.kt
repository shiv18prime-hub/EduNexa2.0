package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SchoolResultRepository {
 private val auth=FirebaseAuth.getInstance();private val db=FirebaseFirestore.getInstance()
 fun students(onResult:(List<Pair<String,String>>)->Unit,onError:(String)->Unit){val sid=auth.currentUser?.uid?:return onError("Please login first");db.collection("users").whereEqualTo("role","student").whereEqualTo("schoolId",sid).get().addOnSuccessListener{snap->onResult(snap.documents.map{it.id to (it.getString("name")?:it.getString("email")?:"Student")})}.addOnFailureListener{onError(it.message?:"Could not load students")}}
 fun publish(studentId:String,studentName:String,exam:String,subject:String,obtained:Double,total:Double,onResult:(Boolean,String)->Unit){val sid=auth.currentUser?.uid?:return onResult(false,"Please login first");if(exam.isBlank()||subject.isBlank()||total<=0||obtained<0||obtained>total)return onResult(false,"Enter valid result details");val p=obtained/total*100;val grade=when{p>=90->"A+";p>=80->"A";p>=70->"B";p>=60->"C";p>=45->"D";p>=33->"E";else->"F"};val data=hashMapOf<String,Any>("schoolId" to sid,"studentId" to studentId,"studentName" to studentName,"exam" to exam.trim(),"subject" to subject.trim(),"obtained" to obtained,"total" to total,"percentage" to p,"grade" to grade,"published" to true,"createdAt" to System.currentTimeMillis());db.collection("schoolResults").add(data).addOnSuccessListener{onResult(true,"Result published")}.addOnFailureListener{onResult(false,it.message?:"Could not publish result")}}
 fun myResults(onResult:(List<Map<String,Any>>)->Unit,onError:(String)->Unit){val uid=auth.currentUser?.uid?:return onError("Please login first");db.collection("schoolResults").whereEqualTo("studentId",uid).whereEqualTo("published",true).get().addOnSuccessListener{snap->onResult(snap.documents.mapNotNull{it.data}.sortedByDescending{(it["createdAt"] as? Number)?.toLong()?:0L})}.addOnFailureListener{onError(it.message?:"Could not load results")}}
}
