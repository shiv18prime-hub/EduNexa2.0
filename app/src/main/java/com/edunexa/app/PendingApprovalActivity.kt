package com.edunexa.app
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class PendingApprovalActivity:EduActivity(){
 private lateinit var status:TextView
 override fun onCreate(state:Bundle?){super.onCreate(state);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,32,32,32)};setContentView(root);root.addView(TextView(this).apply{text="School Verification";textSize=26f});status=TextView(this);root.addView(status);root.addView(Button(this).apply{text="Check Approval";setOnClickListener{check()}});root.addView(Button(this).apply{text="Open General Tools";setOnClickListener{startActivity(Intent(this@PendingApprovalActivity,ToolsActivity::class.java))}});root.addView(Button(this).apply{text="My Profile";setOnClickListener{startActivity(Intent(this@PendingApprovalActivity,MainActivity::class.java).putExtra("profileOnly",true))}});root.addView(Button(this).apply{text="Logout";setOnClickListener{FirebaseAuth.getInstance().signOut();go(AuthActivity::class.java)}})}
 override fun onResume(){super.onResume();check()}
 private fun check(){val uid=FirebaseAuth.getInstance().currentUser?.uid?:return go(AuthActivity::class.java);status.text="Checking your registration…";FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener{d->if(d.getString("role")=="school"&&d.getBoolean("schoolApproved")==true)go(SchoolActivity::class.java)else status.text=if(d.getString("approvalStatus")=="rejected")"Registration was rejected. Contact the EduNexa administrator. General tools remain available." else "Registration received. School services unlock after administrator approval."}.addOnFailureListener{status.text=it.message?:"Could not check approval"}}
 private fun go(target:Class<*>){startActivity(Intent(this,target).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish()}
}
