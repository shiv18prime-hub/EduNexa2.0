package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SchoolResultRepository {
 private val auth=FirebaseAuth.getInstance();private val db=FirebaseFirestore.getInstance()
 fun students(onResult:(List<Pair<String,String>>)->Unit,onError:(String)->Unit) = SchoolRosterRepository().students(onResult,onError)
 fun publish(studentId:String,studentName:String,exam:String,subject:String,obtained:Double,total:Double,onResult:(Boolean,String)->Unit){
  val uid=auth.currentUser?.uid?:return onResult(false,"Please sign in first")
  SchoolScope.resolve({sid->
   val recordRef=db.collection("schoolResults").document()
   db.runTransaction{tx->
    val student=tx.get(db.collection("users").document(studentId))
    check(student.getString("role")=="student"&&student.getString("schoolId")==sid&&student.getString("schoolJoinStatus")=="connected"){"Student is no longer connected"}
    val record=com.edunexa.app.core.SchoolRecords.result(sid,uid,studentId,student.getString("name").orEmpty(),exam,subject,obtained,total,System.currentTimeMillis())
    tx.set(recordRef,record)
   }.addOnSuccessListener{onResult(true,"Result published")}.addOnFailureListener{onResult(false,it.localizedMessage?:"Could not publish result")}
  },{onResult(false,it)})
 }
 fun myResults(onResult:(List<Map<String,Any>>)->Unit,onError:(String)->Unit){val uid=auth.currentUser?.uid?:return onError("Please login first");db.collection("schoolResults").whereEqualTo("studentId",uid).whereEqualTo("published",true).get().addOnSuccessListener{snap->onResult(snap.documents.mapNotNull{it.data}.sortedByDescending{(it["createdAt"] as? Number)?.toLong()?:0L})}.addOnFailureListener{onError(it.message?:"Could not load results")}}
 fun schoolResults(onResult:(List<Map<String,Any>>)->Unit,onError:(String)->Unit){SchoolScope.resolve({sid->db.collection("schoolResults").whereEqualTo("schoolId",sid).get().addOnSuccessListener{snap->onResult(snap.documents.map{d->HashMap<String,Any>(d.data?:emptyMap()).apply{put("id",d.id)}}.sortedByDescending{(it["createdAt"] as? Number)?.toLong()?:0L})}.addOnFailureListener{onError(it.message?:"Could not load school results")}},onError)}
 fun deleteResult(id:String,onResult:(Boolean,String)->Unit){db.collection("schoolResults").document(id).delete().addOnSuccessListener{onResult(true,"Result removed")}.addOnFailureListener{onResult(false,it.message?:"Could not remove result")}}
}
