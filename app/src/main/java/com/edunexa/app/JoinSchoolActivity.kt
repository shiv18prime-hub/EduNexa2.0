package com.edunexa.app

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.edunexa.app.data.SchoolJoinRepository
import com.edunexa.app.data.SchoolWorkflowRepository

class JoinSchoolActivity : EduActivity() {
    private val repo = SchoolJoinRepository()
    private val workflows = SchoolWorkflowRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_school)

        val input = findViewById<EditText>(R.id.schoolCodeInput)
        val button = findViewById<Button>(R.id.joinSchoolBtn)
        val scan = findViewById<Button>(R.id.scanSchoolQrBtn)
        val progress = findViewById<ProgressBar>(R.id.joinProgress)
        val status = findViewById<TextView>(R.id.joinStatus)
        var statusGeneration = 0
        val cancel = Button(this).apply {
            text = "Cancel Pending Request"
            visibility = View.GONE
        }
        val refresh = Button(this).apply { text = "Check Request Status" }
        (button.parent as? ViewGroup)?.addView(cancel)
        (button.parent as? ViewGroup)?.addView(refresh)

        // Temporarily hide QR scanning until the Android scanner integration is verified.
        scan.visibility = View.GONE

        fun showPending() {
            input.isEnabled = false
            button.isEnabled = false
            scan.isEnabled = false
            cancel.visibility = View.VISIBLE
            status.text = "Your request is pending school approval."
        }

        fun checkStatus() {
            val generation = ++statusGeneration
            refresh.isEnabled = false
            status.text = "Checking school connection status…"
            workflows.getJoinStatus { joinStatus, error ->
                if (generation != statusGeneration) return@getJoinStatus
                refresh.isEnabled = true
                if (error.isNotBlank()) {
                    status.text = error
                    return@getJoinStatus
                }
                when (joinStatus) {
                    "pending" -> showPending()
                    "rejected" -> {
                        input.isEnabled = true
                        button.isEnabled = true
                        button.text = "Send Request Again"
                        cancel.visibility = View.GONE
                        status.text = "Previous request was rejected. You can send a new request."
                    }
                    "connected" -> {
                        input.isEnabled = false
                        button.isEnabled = false
                        scan.isEnabled = false
                        cancel.visibility = View.GONE
                        status.text = "You are already connected to a school."
                    }
                    else -> {
                        input.isEnabled = true
                        button.isEnabled = true
                        button.text = "Send Connection Request"
                        cancel.visibility = View.GONE
                        status.text = "No pending school request found. Enter a school code to connect."
                    }
                }
            }
        }

        fun submit(code: String) {
            if (code.isBlank()) {
                input.error = "Enter school code"
                return
            }
            statusGeneration++
            button.isEnabled = false
            refresh.isEnabled = false
            progress.visibility = View.VISIBLE
            status.text = "Verifying school and preparing request…"
            repo.joinByCode(code) { ok, message ->
                refresh.isEnabled = true
                progress.visibility = View.GONE
                status.text = if (ok) "✓ $message" else message
                if (ok && message.contains("Request sent", true)) {
                    showPending()
                } else if (!ok) {
                    button.isEnabled = true
                }
            }
        }

        refresh.setOnClickListener { checkStatus() }
        cancel.setOnClickListener {
            statusGeneration++
            refresh.isEnabled = false
            cancel.isEnabled = false
            workflows.cancel { ok, message ->
                status.text = message
                if (ok) {
                    input.isEnabled = true
                    button.isEnabled = true
                    button.text = "Send Connection Request"
                    scan.isEnabled = false
                    cancel.visibility = View.GONE
                }
                cancel.isEnabled = true
                refresh.isEnabled = true
            }
        }
        button.setOnClickListener { submit(input.text.toString().trim()) }
        checkStatus()
    }
}
