package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AttendanceRepository {
 private val auth=FirebaseAuth.getInstance();private val db=FirebaseFirestore.getInstance()
 fun save(studentId:String,studentName:String,date:String,present:Boolean,onResult:(Boolean,String)->Unit){Backend.call("saveAttendance",mapOf("studentId" to studentId,"date" to date,"present" to present),{onResult(true,it["message"].toString())},{onResult(false,it)})}
 fun schoolStudents(onResult:(List<Pair<String,String>>)->Unit,onError:(String)->Unit){Backend.call("schoolStudents",success={r->onResult(Backend.rows(r["students"]).map{it["id"].toString() to it["name"].toString()})},failure=onError)}
 fun studentAttendance(onResult:(List<Map<String,Any>>)->Unit,onError:(String)->Unit){val uid=auth.currentUser?.uid?:return onError("Please login first");db.collection("attendance").whereEqualTo("studentId",uid).get().addOnSuccessListener{snap->onResult(snap.documents.mapNotNull{it.data}.sortedByDescending{it["date"]?.toString().orEmpty()})}.addOnFailureListener{onError(it.message?:"Could not load attendance")}}
}
