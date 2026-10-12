package com.edunexa.app
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class TeacherActivity:EduActivity(){
 private lateinit var status:TextView;private lateinit var root:LinearLayout
 override fun onCreate(state:Bundle?){super.onCreate(state);root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,24,28,28)};setContentView(ScrollView(this).apply{addView(root)});root.addView(TextView(this).apply{text="Teacher Dashboard";textSize=27f;setTypeface(typeface,android.graphics.Typeface.BOLD)});status=TextView(this);root.addView(status);action("Connect / Change School",JoinSchoolActivity::class.java);action("Publish MCQ Test",PublishMcqActivity::class.java);action("Publish Student Result",PublishResultActivity::class.java);action("Mark Attendance",AttendanceActivity::class.java);action("Publish Notice",PublishNoticeActivity::class.java);action("School Messages",ChatActivity::class.java);action("General Tools",ToolsActivity::class.java);root.addView(Button(this).apply{text="My Profile";setOnClickListener{startActivity(Intent(this@TeacherActivity,MainActivity::class.java).putExtra("profileOnly",true))}});root.addView(Button(this).apply{text="Logout";setOnClickListener{Session.logout(this@TeacherActivity)}})}
 private fun action(label:String,target:Class<*>){root.addView(Button(this).apply{text=label;isAllCaps=false;setOnClickListener{startActivity(Intent(this@TeacherActivity,target))}})}
 override fun onResume(){super.onResume();val uid=FirebaseAuth.getInstance().currentUser?.uid?:return;FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener{d->if(d.getString("role")!="teacher"){finish();return@addOnSuccessListener};status.text=if(d.getString("schoolJoinStatus")=="connected")"Connected to ${d.getString("schoolName")}" else "Connect to your school to use teaching services"}.addOnFailureListener{status.text="Could not load school status"}}
}
