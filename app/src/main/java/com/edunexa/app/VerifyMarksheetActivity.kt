package com.edunexa.app

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class VerifyMarksheetActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var idInput: EditText
    private lateinit var result: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36,36,36,36) }
        root.addView(TextView(this).apply { text="Verify Marksheet"; textSize=28f; setTypeface(typeface,1) })
        root.addView(TextView(this).apply { text="Enter the EduNexa Verification ID printed beside the QR code."; setPadding(0,8,0,20) })
        idInput = EditText(this).apply { hint="Example: EDX-AB12CD34"; isSingleLine=true }
        root.addView(idInput)
        root.addView(Button(this).apply { text="Verify Now"; isAllCaps=false; setOnClickListener { verify() } })
        progress = ProgressBar(this).apply { visibility=android.view.View.GONE }
        root.addView(progress)
        result = TextView(this).apply { textSize=18f; gravity=Gravity.START; setPadding(0,24,0,20) }
        root.addView(result)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun verify() {
        val id=idInput.text.toString().trim().uppercase()
        if(id.isBlank()){ result.text="Enter a Verification ID"; return }
        progress.visibility=android.view.View.VISIBLE; result.text=""
        db.collection("marksheetVerifications").document(id).get().addOnSuccessListener { d ->
            progress.visibility=android.view.View.GONE
            if(!d.exists()){ result.text="NOT FOUND\nNo EduNexa verification record exists for this ID."; return@addOnSuccessListener }
            val approved=d.getBoolean("issuerApproved")==true
            val badge=if(approved) "VERIFIED SCHOOL RECORD" else "UNOFFICIAL / PRACTICE RECORD"
            result.text="$badge\n\nVerification ID: ${d.getString("verificationId") ?: id}\nSchool: ${d.getString("schoolName") ?: "-"}\nStudent: ${d.getString("studentName") ?: "-"}\nRoll No.: ${d.getString("rollNo") ?: "-"}\nClass: ${d.getString("className") ?: "-"}\nExam: ${d.getString("examName") ?: "-"}\nSession: ${d.getString("session") ?: "-"}\nMarks: ${d.getLong("totalObtained") ?: 0} / ${d.getLong("totalMax") ?: 0}\nPercentage: ${String.format("%.2f", d.getDouble("percentage") ?: 0.0)}%\nResult: ${d.getString("result") ?: "-"}"
        }.addOnFailureListener { progress.visibility=android.view.View.GONE; result.text="Verification failed: ${it.message}" }
    }
}
