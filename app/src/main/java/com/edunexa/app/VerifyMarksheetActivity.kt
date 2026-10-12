package com.edunexa.app

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class VerifyMarksheetActivity : EduActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var idInput: EditText
    private lateinit var result: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36,36,36,36) }
        root.addView(TextView(this).apply { text="Verify Marksheet"; textSize=28f; setTypeface(typeface, android.graphics.Typeface.BOLD) })
        root.addView(TextView(this).apply { text="Enter the EduNexa Verification ID printed beside the QR code."; setPadding(0,8,0,20) })
        idInput = EditText(this).apply { hint="Example: EDX-AB12CD34"; isSingleLine=true }
        root.addView(idInput)
        root.addView(Button(this).apply { text="Verify Now"; isAllCaps=false; setOnClickListener { verify() } })
        progress = ProgressBar(this).apply { visibility=android.view.View.GONE }
        root.addView(progress)
        result = TextView(this).apply { textSize=18f; gravity=Gravity.START; setPadding(0,24,0,20) }
        root.addView(result)
        setContentView(ScrollView(this).apply { addView(root) })
        intent.getStringExtra("verificationId")?.let{idInput.setText(it);verify()}
    }

    private fun verify() {
        val id=idInput.text.toString().trim().uppercase()
        if(id.isBlank()){ result.text="Enter a Verification ID"; return }
        progress.visibility=android.view.View.VISIBLE; result.text=""
        com.edunexa.app.data.Backend.call("verifyMarksheet",mapOf("id" to id),{d->
            progress.visibility=android.view.View.GONE
            val badge=if(d["status"]=="revoked")"REVOKED RECORD" else if(d["issuerApproved"]==true)"REGISTERED SCHOOL RECORD" else "PRACTICE RECORD"
            result.text="$badge\n\nID: ${d["verificationId"]}\nSchool: ${d["schoolName"]}\nClass: ${d["className"]}\nExam: ${d["examName"]}\nSession: ${d["session"]}\nMarks: ${d["totalObtained"]}/${d["totalMax"]}\nPercentage: %.2f%%\nResult: ${d["result"]}".format((d["percentage"] as? Number)?.toDouble()?:0.0)
        },{progress.visibility=android.view.View.GONE;result.text=it})
    }
}
