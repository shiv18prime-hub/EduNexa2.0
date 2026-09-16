package com.edunexa.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SchoolActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_school)
        verifySchool()
        findViewById<Button>(R.id.studentsBtn).setOnClickListener { open(StudentManagementActivity::class.java) }
        findViewById<Button>(R.id.resultsBtn).setOnClickListener { open(PublishResultActivity::class.java) }
        findViewById<Button>(R.id.mcqBtn).setOnClickListener { open(PublishMcqActivity::class.java) }
        findViewById<Button>(R.id.attendanceBtn).setOnClickListener { open(AttendanceActivity::class.java) }
        findViewById<Button>(R.id.announcementBtn).setOnClickListener { open(PublishNoticeActivity::class.java) }
        findViewById<Button>(R.id.toolsBtn).setOnClickListener { open(ToolsActivity::class.java) }
        findViewById<Button>(R.id.logoutBtn).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
            finish()
        }
    }

    private fun open(cls: Class<*>) = startActivity(Intent(this, cls))

    private fun verifySchool() {
        val uid = auth.currentUser?.uid ?: return goAuth()
        val status = findViewById<TextView>(R.id.schoolStatus)
        status.text = "Checking school approval…"
        db.collection("users").document(uid).get()
            .addOnSuccessListener { d ->
                if (d.getString("role") != "school" || d.getBoolean("schoolApproved") != true) {
                    Toast.makeText(this, "School services require admin approval", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, PendingApprovalActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                    finish()
                    return@addOnSuccessListener
                }
                val name = d.getString("schoolName") ?: d.getString("name") ?: "EduNexa School"
                status.text = "✓ Verified • $name"
            }
            .addOnFailureListener {
                status.text = "Unable to verify school • Check internet"
                Toast.makeText(this, "Could not verify school account", Toast.LENGTH_LONG).show()
            }
    }

    private fun goAuth() {
        startActivity(Intent(this, AuthActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
        finish()
    }
}
