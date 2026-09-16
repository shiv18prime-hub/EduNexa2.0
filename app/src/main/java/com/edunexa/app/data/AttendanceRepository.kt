package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AttendanceRepository {
 private val auth=FirebaseAuth.getInstance();private val db=FirebaseFirestore.getInstance()
 fun save(studentId:String,studentName:String,date:String,present:Boolean,onResult:(Boolean,String)->Unit){val schoolId=auth.currentUser?.uid?:return onResult(false,"Please login first");val id="${schoolId}_${studentId}_$date";val data=hashMapOf<String,Any>("schoolId" to schoolId,"studentId" to studentId,"studentName" to studentName.trim(),"date" to date,"present" to present,"updatedAt" to System.currentTimeMillis());db.collection("attendance").document(id).set(data).addOnSuccessListener{onResult(true,"Attendance saved")}.addOnFailureListener{onResult(false,it.message?:"Could not save attendance")}}
 fun schoolStudents(onResult:(List<Pair<String,String>>)->Unit,onError:(String)->Unit){val schoolId=auth.currentUser?.uid?:return onError("Please login first");db.collection("users").whereEqualTo("role","student").whereEqualTo("schoolId",schoolId).get().addOnSuccessListener{snap->onResult(snap.documents.map{it.id to (it.getString("name")?:it.getString("email")?:"Student")})}.addOnFailureListener{onError(it.message?:"Could not load students")}}
 fun studentAttendance(onResult:(List<Map<String,Any>>)->Unit,onError:(String)->Unit){val uid=auth.currentUser?.uid?:return onError("Please login first");db.collection("attendance").whereEqualTo("studentId",uid).get().addOnSuccessListener{snap->onResult(snap.documents.mapNotNull{it.data}.sortedByDescending{it["date"]?.toString().orEmpty()})}.addOnFailureListener{onError(it.message?:"Could not load attendance")}}
}
