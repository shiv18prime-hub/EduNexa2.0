package com.edunexa.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.SchoolJoinRepository

class JoinSchoolActivity : AppCompatActivity() {
    private val repo = SchoolJoinRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_school)
        val input = findViewById<EditText>(R.id.schoolCodeInput)
        val button = findViewById<Button>(R.id.joinSchoolBtn)
        val progress = findViewById<ProgressBar>(R.id.joinProgress)
        val status = findViewById<TextView>(R.id.joinStatus)

        button.setOnClickListener {
            val code = input.text.toString().trim()
            if (code.isBlank()) { input.error = "Enter school code"; return@setOnClickListener }
            button.isEnabled = false; progress.visibility = View.VISIBLE; status.text = "Verifying school…"
            repo.joinByCode(code) { ok, message ->
                progress.visibility = View.GONE; button.isEnabled = true
                status.text = if (ok) "✓ $message" else message
                if (ok) input.isEnabled = false
            }
        }
    }
}
