package com.edunexa.app

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.edunexa.app.data.SchoolJoinRepository
import com.edunexa.app.data.SchoolWorkflowRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.android.gms.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

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
        val cancel = Button(this).apply {
            text = "Cancel Pending Request"
            visibility = View.GONE
        }
        (button.parent as? ViewGroup)?.addView(cancel)

        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    when (doc.getString("schoolJoinStatus")) {
                        "pending" -> {
                            cancel.visibility = View.VISIBLE
                            input.isEnabled = false
                            button.isEnabled = false
                            scan.isEnabled = false
                            status.text = "Your request is pending school approval."
                        }
                        "rejected" -> {
                            button.text = "Send Request Again"
                            status.text = "Previous request was rejected. You can send a new request."
                        }
                    }
                }
        }

        fun submit(code: String) {
            if (code.isBlank()) {
                input.error = "Enter school code"
                return
            }
            button.isEnabled = false
            scan.isEnabled = false
            progress.visibility = View.VISIBLE
            status.text = "Verifying school and preparing request…"
            repo.joinByCode(code) { ok, message ->
                progress.visibility = View.GONE
                button.isEnabled = true
                scan.isEnabled = true
                status.text = if (ok) "✓ $message" else message
                if (ok && message.contains("Request sent", true)) {
                    input.isEnabled = false
                    cancel.visibility = View.VISIBLE
                    button.isEnabled = false
                    scan.isEnabled = false
                }
            }
        }

        cancel.setOnClickListener {
            cancel.isEnabled = false
            workflows.cancel { ok, message ->
                status.text = message
                if (ok) {
                    input.isEnabled = true
                    button.isEnabled = true
                    scan.isEnabled = true
                    cancel.visibility = View.GONE
                }
                cancel.isEnabled = true
            }
        }
        button.setOnClickListener { submit(input.text.toString().trim()) }

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        val scanner = GmsBarcodeScanning.getClient(this, options)
        scan.setOnClickListener {
            status.text = "Opening secure QR scanner…"
            scanner.startScan().addOnSuccessListener { barcode ->
                val raw = barcode.rawValue.orEmpty().trim()
                val code = when {
                    raw.startsWith("EDUNEXA:SCHOOL:", true) -> raw.substringAfterLast(":")
                    raw.startsWith("EDU-", true) -> raw
                    else -> ""
                }
                if (code.isBlank()) {
                    status.text = "This is not an EduNexa school QR."
                } else {
                    input.setText(code)
                    submit(code)
                }
            }.addOnCanceledListener {
                status.text = "Scan cancelled"
            }.addOnFailureListener { error ->
                status.text = error.message ?: "QR scanner unavailable"
            }
        }
    }
}
