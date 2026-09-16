package com.edunexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.UUID

class MarksheetMakerActivity : AppCompatActivity() {
    private lateinit var school: EditText
    private lateinit var address: EditText
    private lateinit var student: EditText
    private lateinit var roll: EditText
    private lateinit var clazz: EditText
    private lateinit var exam: EditText
    private lateinit var session: EditText
    private lateinit var subjects: EditText
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var save: Button

    private var logo: Bitmap? = null
    private var sheet: Bitmap? = null
    private var unsaved = false
    private var verificationId: String? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        r.addView(TextView(this).apply {
            text = "Professional Marksheet Maker"
            textSize = 26f
            setTypeface(typeface, 1)
        })
        r.addView(TextView(this).apply {
            text = "School details • Logo • Result • Firebase verification • QR • Preview → Save PDF"
            setPadding(0, 8, 0, 18)
        })

        fun field(hintText: String) = EditText(this).apply {
            hint = hintText
            r.addView(this)
        }

        school = field("School Name")
        address = field("School Address / Contact / Code")
        student = field("Student Name")
        roll = field("Roll / Admission No.")
        clazz = field("Class / Section")
        exam = field("Exam Name")
        session = field("Academic Session")
        subjects = field("Subjects: Hindi:100:75, English:100:82, Math:100:90")

        r.addView(Button(this).apply {
            text = "Choose School Logo"
            isAllCaps = false
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }, 401)
            }
        })
        r.addView(Button(this).apply {
            text = "Generate & Register Preview"
            isAllCaps = false
            setOnClickListener { generate() }
        })

        preview = ImageView(this).apply {
            adjustViewBounds = true
            visibility = View.GONE
        }
        r.addView(preview, LinearLayout.LayoutParams(-1, 900))

        save = Button(this).apply {
            text = "Save Marksheet PDF"
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "application/pdf"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_TITLE, "EduNexa_Marksheet_${verificationId ?: System.currentTimeMillis()}.pdf")
                }, 402)
            }
        }
        r.addView(save)

        status = TextView(this).apply { setPadding(0, 16, 0, 20) }
        r.addView(status)
        setContentView(ScrollView(this).apply { addView(r) })
    }

    override fun onActivityResult(q: Int, res: Int, data: Intent?) {
        super.onActivityResult(q, res, data)
        if (res != Activity.RESULT_OK) return
        when (q) {
            401 -> {
                logo = data?.data?.let { uri ->
                    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
                status.text = "School logo selected"
            }
            402 -> {
                val uri = data?.data ?: return
                val bmp = sheet ?: return
                val pdf = PdfDocument()
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(bmp.width, bmp.height, 1).create())
                page.canvas.drawBitmap(bmp, 0f, 0f, null)
                pdf.finishPage(page)
                runCatching {
                    contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
                    unsaved = false
                    status.text = "Marksheet PDF saved • Verification ID: $verificationId"
                }.onFailure {
                    status.text = "Save failed: ${it.message}"
                }
                pdf.close()
            }
        }
    }

    private fun generate() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            status.text = "Login required"
            return
        }
        if (school.text.isBlank() || student.text.isBlank() || subjects.text.isBlank()) {
            status.text = "Enter school, student and subject marks"
            return
        }
        val rows = parseSubjects()
        if (rows.isEmpty()) {
            status.text = "Use format: Math:100:80, Hindi:100:70"
            return
        }

        status.text = "Checking school verification..."
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val approved = userDoc.getString("role") == "school" && userDoc.getBoolean("schoolApproved") == true
                val id = "EDX-" + UUID.randomUUID().toString().take(8).uppercase()
                verificationId = id
                val maxTotal = rows.sumOf { it.second }
                val gotTotal = rows.sumOf { it.third }
                val pct = gotTotal * 100.0 / maxTotal
                val result = if (rows.all { it.third * 100 / it.second >= 33 }) "PASS" else "FAIL"
                val record = hashMapOf<String, Any>(
                    "verificationId" to id,
                    "issuerUid" to uid,
                    "issuerApproved" to approved,
                    "schoolName" to school.text.toString(),
                    "studentName" to student.text.toString(),
                    "rollNo" to roll.text.toString(),
                    "className" to clazz.text.toString(),
                    "examName" to exam.text.toString(),
                    "session" to session.text.toString(),
                    "totalObtained" to gotTotal,
                    "totalMax" to maxTotal,
                    "percentage" to pct,
                    "result" to result,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("marksheetVerifications").document(id).set(record)
                    .addOnSuccessListener {
                        drawSheet(rows, id, maxTotal, gotTotal, pct, result, approved)
                    }
                    .addOnFailureListener {
                        status.text = "Could not register verification: ${it.message}"
                    }
            }
            .addOnFailureListener {
                status.text = "Could not check school account: ${it.message}"
            }
    }

    private fun drawSheet(
        rows: List<Triple<String, Int, Int>>,
        id: String,
        maxTotal: Int,
        gotTotal: Int,
        pct: Double,
        result: String,
        approved: Boolean
    ) {
        val qrPayload = "EDUNEXA_VERIFY:$id"
        val width = 1240
        val height = 1754
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(1).apply { color = Color.rgb(25, 45, 90) }

        canvas.drawRect(0f, 0f, width.toFloat(), 18f, paint)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 54f
        canvas.drawText(school.text.toString(), width / 2f, 105f, paint)
        paint.textSize = 25f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(address.text.toString(), width / 2f, 145f, paint)
        logo?.let { canvas.drawBitmap(Bitmap.createScaledBitmap(it, 120, 120, true), 55f, 45f, null) }
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 36f
        canvas.drawText("ACADEMIC MARKSHEET", width / 2f, 215f, paint)
        paint.textSize = 22f
        canvas.drawText(if (approved) "EDUNEXA VERIFIED SCHOOL RECORD" else "UNOFFICIAL / PRACTICE MARKSHEET", width / 2f, 250f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 25f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Student: ${student.text}", 65f, 310f, paint)
        canvas.drawText("Roll/Admission: ${roll.text}", 650f, 310f, paint)
        canvas.drawText("Class: ${clazz.text}", 65f, 355f, paint)
        canvas.drawText("Exam: ${exam.text}   Session: ${session.text}", 650f, 355f, paint)

        var y = 435f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("SUBJECT", 70f, y, paint)
        canvas.drawText("MAX", 650f, y, paint)
        canvas.drawText("OBTAINED", 820f, y, paint)
        canvas.drawText("GRADE", 1040f, y, paint)
        paint.typeface = Typeface.DEFAULT
        rows.forEach { (name, max, got) ->
            y += 55f
            canvas.drawText(name, 70f, y, paint)
            canvas.drawText(max.toString(), 650f, y, paint)
            canvas.drawText(got.toString(), 820f, y, paint)
            canvas.drawText(grade(got, max), 1040f, y, paint)
        }

        y += 90f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 29f
        canvas.drawText("Total: $gotTotal / $maxTotal", 70f, y, paint)
        canvas.drawText("Percentage: %.2f%%".format(pct), 430f, y, paint)
        canvas.drawText("Result: $result", 850f, y, paint)

        val qr = makeQr(qrPayload, 230)
        canvas.drawBitmap(qr, 70f, height - 430f, null)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 21f
        canvas.drawText("Scan QR / enter ID in EduNexa verification", 320f, height - 350f, paint)
        canvas.drawText("Verification ID: $id", 320f, height - 310f, paint)
        canvas.drawText("Class Teacher Signature", 70f, height - 150f, paint)
        canvas.drawText("School Stamp", 500f, height - 150f, paint)
        canvas.drawText("Principal Signature", 900f, height - 150f, paint)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 18f
        canvas.drawText("EduNexa verification record", width / 2f, height - 65f, paint)

        sheet = bitmap
        preview.setImageBitmap(bitmap)
        preview.visibility = View.VISIBLE
        save.visibility = View.VISIBLE
        unsaved = true
        status.text = "Registered • ${if (approved) "Verified school record" else "Unofficial/practice record"} • ID: $id • PDF not saved yet."
    }

    private fun makeQr(text: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }

    private fun parseSubjects(): List<Triple<String, Int, Int>> =
        subjects.text.toString().split(',').mapNotNull {
            val parts = it.trim().split(':')
            if (parts.size != 3) return@mapNotNull null
            val max = parts[1].toIntOrNull()
            val got = parts[2].toIntOrNull()
            if (max != null && got != null && max > 0 && got in 0..max) {
                Triple(parts[0].trim(), max, got)
            } else null
        }

    private fun grade(got: Int, max: Int) = when (got * 100 / max) {
        in 90..100 -> "A+"
        in 80..89 -> "A"
        in 70..79 -> "B+"
        in 60..69 -> "B"
        in 50..59 -> "C"
        in 33..49 -> "D"
        else -> "F"
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (unsaved) {
            AlertDialog.Builder(this)
                .setTitle("Discard unsaved PDF?")
                .setMessage("The verification record is registered, but this PDF has not been saved to your device.")
                .setPositiveButton("Discard PDF") { _, _ ->
                    unsaved = false
                    super.onBackPressed()
                }
                .setNegativeButton("Stay", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}
