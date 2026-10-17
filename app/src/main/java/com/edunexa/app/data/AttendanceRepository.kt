package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AttendanceRepository {
 private val auth=FirebaseAuth.getInstance();private val db=FirebaseFirestore.getInstance()
 fun save(studentId:String,studentName:String,date:String,present:Boolean,onResult:(Boolean,String)->Unit){
  val uid=auth.currentUser?.uid?:return onResult(false,"Please sign in first")
  val day=date.trim()
  try{com.edunexa.app.core.SchoolRecords.attendance("",uid,studentId,"",day,present,0)}catch(e:IllegalArgumentException){return onResult(false,e.message?:"Enter a valid date")}
  SchoolScope.resolve({sid->
   db.runTransaction{tx->
    val student=tx.get(db.collection("users").document(studentId))
    check(student.getString("role")=="student"&&student.getString("schoolId")==sid&&student.getString("schoolJoinStatus")=="connected"){"Student is no longer connected"}
    tx.set(db.collection("attendance").document("${sid}_${studentId}_$day"),com.edunexa.app.core.SchoolRecords.attendance(sid,uid,studentId,student.getString("name").orEmpty(),day,present,System.currentTimeMillis()))
   }.addOnSuccessListener{onResult(true,"Attendance saved for $day")}.addOnFailureListener{onResult(false,it.localizedMessage?:"Could not save attendance")}
  },{onResult(false,it)})
 }
 fun schoolStudents(onResult:(List<Pair<String,String>>)->Unit,onError:(String)->Unit) = SchoolRosterRepository().students(onResult,onError)
 fun studentAttendance(onResult:(List<Map<String,Any>>)->Unit,onError:(String)->Unit){val uid=auth.currentUser?.uid?:return onError("Please login first");db.collection("attendance").whereEqualTo("studentId",uid).get().addOnSuccessListener{snap->onResult(snap.documents.mapNotNull{it.data}.sortedByDescending{it["date"]?.toString().orEmpty()})}.addOnFailureListener{onError(it.message?:"Could not load attendance")}}
}
