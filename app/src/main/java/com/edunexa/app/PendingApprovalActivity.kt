package com.edunexa.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PendingApprovalActivity:EduActivity(){
    private lateinit var status:TextView
    private val db=FirebaseFirestore.getInstance()
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,32,32,32)}
        setContentView(root)
        root.addView(TextView(this).apply{text="School Verification";textSize=26f})
        status=TextView(this);root.addView(status)
        root.addView(Button(this).apply{text="Check Approval";setOnClickListener{check()}})
        root.addView(Button(this).apply{text="Open General Tools";setOnClickListener{startActivity(Intent(this@PendingApprovalActivity,ToolsActivity::class.java))}})
        root.addView(Button(this).apply{text="My Profile";setOnClickListener{startActivity(Intent(this@PendingApprovalActivity,MainActivity::class.java).putExtra("profileOnly",true))}})
        root.addView(Button(this).apply{text="Logout";setOnClickListener{Session.logout(this@PendingApprovalActivity)}})
    }
    override fun onResume(){super.onResume();check()}
    private fun check(){
        val uid=FirebaseAuth.getInstance().currentUser?.uid?:return go(AuthActivity::class.java)
        status.text="Checking your registration…"
        db.collection("users").document(uid).get().addOnSuccessListener{profile->
            if(profile.getString("role")!="school"){
                status.text=if(profile.getString("approvalStatus")=="rejected")"Registration was rejected. Contact the EduNexa administrator. General tools remain available." else "Registration received. School services unlock after administrator approval."
                return@addOnSuccessListener
            }
            db.collection("schoolApprovals").document(uid).get().addOnSuccessListener{approval->
                if(profile.getBoolean("schoolApproved")==true&&approval.getBoolean("approved")==true)go(SchoolActivity::class.java)
                else status.text=if(profile.getString("approvalStatus")=="rejected")"Registration was rejected. Contact the EduNexa administrator. General tools remain available." else "Administrator verification required. General tools remain available."
            }.addOnFailureListener{status.text=it.message?:"Could not check approval"}
        }.addOnFailureListener{status.text=it.message?:"Could not check registration"}
    }
    private fun go(target:Class<*>){startActivity(Intent(this,target).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish()}
}
