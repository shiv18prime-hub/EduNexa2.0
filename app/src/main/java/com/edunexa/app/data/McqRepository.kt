package com.edunexa.app.data
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class McqRepository {
    private val auth=FirebaseAuth.getInstance(); private val db=FirebaseFirestore.getInstance()
    fun publishTest(title:String,subject:String,chapter:String,durationMinutes:Int,questions:List<Map<String,Any>>,onResult:(Boolean,String)->Unit){Backend.call("publishMcq",mapOf("title" to title,"subject" to subject,"chapter" to chapter,"durationMinutes" to durationMinutes,"questions" to questions),{onResult(true,it["message"].toString())},{onResult(false,it)})}
    fun studentTests(onResult:(List<Pair<String,Map<String,Any>>>)->Unit,onError:(String)->Unit){Backend.call("studentTests",success={r->onResult(Backend.rows(r["tests"]).map{it["id"].toString() to it})},failure=onError)}
    fun start(testId:String,onResult:(Map<String,Any?>)->Unit,onError:(String)->Unit){Backend.call("startMcq",mapOf("testId" to testId),onResult,onError)}
    fun submit(sessionId:String,answers:Map<String,Int>,onResult:(Map<String,Any?>)->Unit,onError:(String)->Unit){Backend.call("submitMcq",mapOf("sessionId" to sessionId,"answers" to answers),onResult,onError)}
    fun attemptHistory(onResult:(List<Map<String,Any>>)->Unit,onError:(String)->Unit){val uid=auth.currentUser?.uid?:return onError("Please login first");db.collection("mcqAttempts").whereEqualTo("studentId",uid).get().addOnSuccessListener{snap->onResult(snap.documents.mapNotNull{it.data}.sortedByDescending{(it["submittedAt"] as? Number)?.toLong()?:0L})}.addOnFailureListener{onError(it.message?:"Could not load history")}}
    fun schoolAnalytics(onResult:(List<Map<String,Any>>)->Unit,onError:(String)->Unit){SchoolScope.resolve({sid->db.collection("mcqAttempts").whereEqualTo("schoolId",sid).get().addOnSuccessListener{onResult(it.documents.mapNotNull{d->d.data})}.addOnFailureListener{onError(it.message?:"Could not load analytics")}},onError)}
}
