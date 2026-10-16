package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Transactional school approval and membership operations that run on Firebase Spark. */
class SchoolWorkflowRepository {
    private val auth=FirebaseAuth.getInstance()
    private val db=FirebaseFirestore.getInstance()
    private fun uid()=auth.currentUser?.uid

    fun join(codeInput:String,done:(Boolean,String)->Unit){
        val studentId=uid()?:return done(false,"Please sign in first")
        val code=codeInput.trim().uppercase().take(50)
        if(code.isBlank())return done(false,"Enter a school code")
        val userRef=db.collection("users").document(studentId)
        val codeRef=db.collection("schoolCodes").document(code)
        db.runTransaction{tx->
            val me=tx.get(userRef);val schoolCode=tx.get(codeRef)
            val schoolId=schoolCode.getString("schoolId")?:throw IllegalStateException("Invalid or disabled school code")
            val schoolRef=db.collection("users").document(schoolId)
            val approvalRef=db.collection("schoolApprovals").document(schoolId)
            val memberRef=db.collection("memberships").document("${schoolId}_$studentId")
            val requestRef=db.collection("schoolJoinRequests").document("${schoolId}_$studentId")
            val school=tx.get(schoolRef);val approval=tx.get(approvalRef);val membership=tx.get(memberRef)
            val request=tx.get(requestRef)
            if(!me.exists()||me.getString("role") !in listOf("student","teacher"))throw IllegalStateException("Student or teacher account required")
            if(schoolCode.getBoolean("active")!=true||school.getString("role")!="school"||school.getBoolean("schoolApproved")!=true||approval.getBoolean("approved")!=true)throw IllegalStateException("School needs administrator approval")
            if(me.getString("schoolJoinStatus")=="pending")throw IllegalStateException("A school request is already pending")
            if(me.getString("schoolId")==schoolId&&me.getString("schoolJoinStatus")=="connected"&&membership.getBoolean("active")==true&&membership.getString("role")==me.getString("role"))return@runTransaction "Already connected"
            val name=me.getString("name").orEmpty();val role=me.getString("role").orEmpty()
            tx.set(requestRef,mapOf("schoolId" to schoolId,"schoolName" to (school.getString("schoolName")?.ifBlank{null}?:school.getString("name").orEmpty()),
                "studentId" to studentId,"studentName" to name,"studentEmail" to (me.getString("email")?:auth.currentUser?.email.orEmpty()),
                "className" to (me.getString("className")?:""),"memberRole" to role,"schoolCode" to code,
                "status" to "pending","createdAt" to System.currentTimeMillis()))
            tx.update(userRef,mapOf("schoolJoinStatus" to "pending","pendingSchoolId" to schoolId,"pendingSchoolCode" to code))
            "Request sent. Your school will review it."
        }.addOnSuccessListener{done(true,it)}.addOnFailureListener{done(false,it.message?:"Could not send school request")}
    }

    fun cancel(done:(Boolean,String)->Unit){
        val memberId=uid()?:return done(false,"Please sign in first")
        val userRef=db.collection("users").document(memberId)
        db.runTransaction{tx->
            val me=tx.get(userRef);if(me.getString("schoolJoinStatus")!="pending")throw IllegalStateException("No pending request")
            val schoolId=me.getString("pendingSchoolId").orEmpty();if(schoolId.isBlank())throw IllegalStateException("Pending school is missing")
            val requestRef=db.collection("schoolJoinRequests").document("${schoolId}_$memberId")
            val request=tx.get(requestRef)
            if(request.exists()&&request.getString("status")=="pending")tx.update(requestRef,mapOf("status" to "cancelled","reviewedAt" to System.currentTimeMillis()))
            tx.update(userRef,mapOf("schoolJoinStatus" to if(me.getString("schoolId").isNullOrBlank())"" else "connected","pendingSchoolId" to "","pendingSchoolCode" to ""))
            "Request cancelled"
        }.addOnSuccessListener{done(true,it)}.addOnFailureListener{done(false,it.message?:"Could not cancel request")}
    }

    fun review(studentId:String,approve:Boolean,done:(Boolean,String)->Unit){
        val schoolId=uid()?:return done(false,"Please sign in as the school")
        val schoolRef=db.collection("users").document(schoolId)
        val approvalRef=db.collection("schoolApprovals").document(schoolId)
        val userRef=db.collection("users").document(studentId)
        val requestRef=db.collection("schoolJoinRequests").document("${schoolId}_$studentId")
        val membershipRef=db.collection("memberships").document("${schoolId}_$studentId")
        db.runTransaction{tx->
            val school=tx.get(schoolRef);val approval=tx.get(approvalRef);val request=tx.get(requestRef);val member=tx.get(userRef);tx.get(membershipRef)
            if(school.getString("role")!="school"||school.getBoolean("schoolApproved")!=true||approval.getBoolean("approved")!=true)throw IllegalStateException("Only an approved school can review requests")
            if(request.getString("status")!="pending"||request.getString("studentId")!=studentId||member.getString("pendingSchoolId")!=schoolId||member.getString("role") !in listOf("student","teacher"))throw IllegalStateException("No current pending request")
            val role=member.getString("role").orEmpty()
            if(approve){
                tx.set(membershipRef,mapOf("uid" to studentId,"schoolId" to schoolId,"role" to role,"active" to true,"approvedBy" to schoolId,"name" to member.getString("name").orEmpty(),"createdAt" to System.currentTimeMillis()))
                tx.update(userRef,mapOf("schoolId" to schoolId,"schoolName" to (school.getString("schoolName")?.ifBlank{null}?:school.getString("name").orEmpty()),
                    "schoolCode"