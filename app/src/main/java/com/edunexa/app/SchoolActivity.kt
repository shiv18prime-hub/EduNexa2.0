package com.edunexa.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SchoolActivity:EduActivity(){
    private val db=FirebaseFirestore.getInstance()
    private val auth=FirebaseAuth.getInstance()
    private lateinit var schoolName:TextView
    private lateinit var schoolStatus:TextView
    private lateinit var schoolMeta:TextView
    private lateinit var studentCount:TextView
    private lateinit var requestCount:TextView
    private lateinit var dashboardHint:TextView
    private lateinit var serviceButtons:List<Button>
    private var verificationGeneration = 0

    override fun onCreate(b:Bundle?){
        super.onCreate(b);setContentView(R.layout.activity_school)
        schoolName=findViewById(R.id.schoolName);schoolStatus=findViewById(R.id.schoolStatus);schoolMeta=findViewById(R.id.schoolMeta)
        studentCount=findViewById(R.id.studentCount);requestCount=findViewById(R.id.requestCount);dashboardHint=findViewById(R.id.dashboardHint)
        serviceButtons=listOf(findViewById(R.id.studentsBtn),findViewById(R.id.resultsBtn),findViewById(R.id.mcqBtn),findViewById(R.id.attendanceBtn),findViewById(R.id.announcementBtn),findViewById(R.id.qrBtn),findViewById(R.id.toolsBtn),findViewById(R.id.chatBtn))
        serviceButtons.forEach{it.isEnabled=false}
        findViewById<Button>(R.id.dashboardRefreshBtn).setOnClickListener{verifySchool()}
        findViewById<Button>(R.id.studentsBtn).setOnClickListener{open(StudentManagementActivity::class.java)}
        findViewById<Button>(R.id.resultsBtn).setOnClickListener{open(PublishResultActivity::class.java)}
        findViewById<Button>(R.id.mcqBtn).apply {
            text=if(BuildConfig.HOSTED_SERVICES_ENABLED)"Publish MCQ Test" else "Practice Quizzes"
            setOnClickListener{open(if(BuildConfig.HOSTED_SERVICES_ENABLED)PublishMcqActivity::class.java else PracticeActivity::class.java)}
        }
        findViewById<Button>(R.id.attendanceBtn).setOnClickListener{open(AttendanceActivity::class.java)}
        findViewById<Button>(R.id.announcementBtn).setOnClickListener{open(PublishNoticeActivity::class.java)}
        findViewById<Button>(R.id.qrBtn).setOnClickListener{open(QrCenterActivity::class.java)}
        findViewById<Button>(R.id.chatBtn).setOnClickListener{open(ChatActivity::class.java)}
        findViewById<Button>(R.id.toolsBtn).setOnClickListener{open(ToolsActivity::class.java)}
        findViewById<Button>(R.id.logoutBtn).setOnClickListener{Session.logout(this@SchoolActivity)}
        verifySchool()
    }
    override fun onResume(){super.onResume();if(::schoolStatus.isInitialized&&auth.currentUser!=null)verifySchool()}
    private fun open(c:Class<*>)=startActivity(Intent(this,c))
    private fun lockForApproval(){serviceButtons.forEach{it.isEnabled=false};findViewById<Button>(R.id.toolsBtn).isEnabled=true;schoolStatus.text="Administrator verification required";dashboardHint.text="Ask the EduNexa administrator to verify this school."}
    private fun verifySchool(){
        val uid=auth.currentUser?.uid?:return goAuth();val generation=++verificationGeneration;schoolStatus.text="Checking school approval…";dashboardHint.text="Verifying secure school access…"
        val profileRef=db.collection("users").document(uid)
        profileRef.get().addOnSuccessListener{profile->
            if(generation!=verificationGeneration)return@addOnSuccessListener
            if(profile.getString("role")!="school"||profile.getBoolean("schoolApproved")!=true){lockForApproval();Toast.makeText(this,"School services require admin approval",Toast.LENGTH_LONG).show();return@addOnSuccessListener}
            db.collection("schoolApprovals").document(uid).get().addOnSuccessListener{approval->
                if(generation!=verificationGeneration)return@addOnSuccessListener
                if(approval.getBoolean("approved")!=true){lockForApproval();return@addOnSuccessListener}
                schoolName.text=profile.getString("schoolName")?:profile.getString("name")?:"EduNexa School"
                schoolStatus.text="✓ Admin Verified School"
                schoolMeta.text=listOf(profile.getString("address").orEmpty(),profile.getString("contactNumber").orEmpty()).filter{it.isNotBlank()}.joinToString(" • ").ifBlank{"EduNexa School Account"}
                dashboardHint.text="Manage connected students, publish results and notices, and record attendance from your school workspace."
                serviceButtons.forEach{it.isEnabled=true}
                loadDashboardCounts(uid,generation)
            }.addOnFailureListener{if(generation==verificationGeneration){lockForApproval();Toast.makeText(this,it.message?:"Could not check school approval",Toast.LENGTH_LONG).show()}}
        }.addOnFailureListener{if(generation==verificationGeneration){lockForApproval();Toast.makeText(this,it.message?:"Could not load school account",Toast.LENGTH_LONG).show()}}
    }
    private fun loadDashboardCounts(uid:String,generation:Int){
        studentCount.text="…";requestCount.text="…"
        db.collection("memberships").whereEqualTo("role","student").whereEqualTo("schoolId",uid).whereEqualTo("active",true).get()
            .addOnSuccessListener{if(generation==verificationGeneration)studentCount.text=it.size().toString()}.addOnFailureListener{if(generation==verificationGeneration)studentCount.text="—"}
        db.collection("schoolJoinRequests").whereEqualTo("schoolId",uid).whereEqualTo("status","pending").get()
            .addOnSuccessListener{if(generation==verificationGeneration)requestCount.text=it.size().toString()}.addOnFailureListener{if(generation==verificationGeneration)requestCount.text="—"}
    }
    private fun goAuth(){startActivity(Intent(this,AuthActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish()}
}
