package com.edunexa.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.SchoolJoinRepository
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

class JoinSchoolActivity : AppCompatActivity() {
    private val repo = SchoolJoinRepository()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_join_school)
        val input = findViewById<EditText>(R.id.schoolCodeInput); val button = findViewById<Button>(R.id.joinSchoolBtn)
        val scan = findViewById<Button>(R.id.scanSchoolQrBtn); val progress = findViewById<ProgressBar>(R.id.joinProgress); val status = findViewById<TextView>(R.id.joinStatus)
        fun submit(code: String) {
            if (code.isBlank()) { input.error = "Enter school code"; return }
            button.isEnabled = false; scan.isEnabled = false; progress.visibility = View.VISIBLE; status.text = "Verifying school and preparing request…"
            repo.joinByCode(code) { ok, message -> progress.visibility = View.GONE; button.isEnabled = true; scan.isEnabled = true; status.text = if (ok) "✓ $message" else message; if (ok) input.isEnabled = false }
        }
        button.setOnClickListener { submit(input.text.toString().trim()) }
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build()
        val scanner = GmsBarcodeScanning.getClient(this, options)
        scan.setOnClickListener {
            status.text = "Opening secure QR scanner…"
            scanner.startScan().addOnSuccessListener { barcode ->
                val raw = barcode.rawValue.orEmpty().trim(); val code = when {
                    raw.startsWith("EDUNEXA:SCHOOL:", true) -> raw.substringAfterLast(":")
                    raw.startsWith("EDU-", true) -> raw
                    else -> ""
                }
                if (code.isBlank()) status.text = "This is not an EduNexa school QR." else { input.setText(code); submit(code) }
            }.addOnCanceledListener { status.text = "Scan cancelled" }.addOnFailureListener { e -> status.text = e.message ?: "QR scanner unavailable" }
        }
    }
}
