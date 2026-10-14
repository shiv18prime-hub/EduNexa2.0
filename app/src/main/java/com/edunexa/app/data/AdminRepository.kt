package com.edunexa.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.security.SecureRandom

class AdminRepository {
    private val db = FirebaseFirestore.getInstance()

    fun pendingSchools(onResult: (List<UserProfile>) -> Unit, onError: (String) -> Unit) {
        db.collection("users").whereEqualTo("role", "school").whereEqualTo("schoolApproved", false).get()
            .addOnSuccessListener { snap -> onResult(snap.toObjects(UserProfile::class.java)) }
            .addOnFailureListener { onError(it.message ?: "Could not load schools") }
    }

    fun setSchoolApproval(uid:String,approved:Boolean,onResult:(Boolean,String)->Unit){
        val actor=com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return onResult(false,"Please sign in as administrator")
        val alphabet="ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random=SecureRandom()
        val code="EDU-"+(1..10).map{alphabet[random.nextInt(alphabet.length)]}.joinToString("")
        val schoolRef=db.collection("users").document(uid)
        val approvalRef=db.collection("schoolApprovals").document(uid)
        val codeRef=db.collection("schoolCodes").document(code)
        db.runTransaction { tx ->
            val school=tx.get(schoolRef)
            if(!school.exists()||school.getString("role")!="school") throw IllegalStateException("School account not found")
            val previous=school.getString("schoolCode")
            val oldRef=previous?.takeIf{it.isNotBlank()}?.let{db.collection("schoolCodes").document(it)}
            val old=oldRef?.let{tx.get(it)}
            tx.update(schoolRef,mapOf("schoolApproved" to approved,"approvalStatus" to if(approved)"approved" else "rejected","schoolCode" to code))
            tx.set(approvalRef,mapOf("approved" to approved,"approvedBy" to actor,"updatedAt" to System.currentTimeMillis()))
            if(old?.exists()==true&&oldRef!=null)tx.update(oldRef,"active",false)
            tx.set(codeRef,mapOf("schoolId" to uid,"schoolName" to (school.getString("schoolName")?.takeIf{it.isNotBlank()}?:school.getString("name").orEmpty()),"code" to code,"active" to approved,"createdAt" to System.currentTimeMillis()))
            code
        }.addOnSuccessListener{onResult(true,"${if(approved)"School approved" else "School rejected"} • $it")}
            .addOnFailureListener{onResult(false,it.message?:"Could not update school approval")}
    }
}
