package com.edunexa.app

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SchoolActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_school)
        verifySchool()
        findViewById<Button>(R.id.studentsBtn).setOnClickListener { message("Student management is ready for the next data screen") }
        findViewById<Button>(R.id.resultsBtn).setOnClickListener { message("Results workspace") }
        findViewById<Button>(R.id.attendanceBtn).setOnClickListener { message("Attendance workspace") }
        findViewById<Button>(R.id.announcementBtn).setOnClickListener { message("Announcements workspace") }
        findViewById<Button>(R.id.toolsBtn).setOnClickListener { message("General tools remain available") }
    }

    private fun verifySchool() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return finish()
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val role = doc.getString("role")
            val approved = doc.getBoolean("schoolApproved") == true
            if (role != "school" || !approved) {
                Toast.makeText(this, "School services require admin approval", Toast.LENGTH_LONG).show()
                finish()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Unable to verify school account", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
