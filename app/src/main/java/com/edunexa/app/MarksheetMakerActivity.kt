package com.edunexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.text.*
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
    data class Subject(val name: String, val max: Int, val got: Int, val pass: Int, val optional: Boolean)

    private lateinit var school: EditText
    private lateinit var address: EditText
    private lateinit var student: EditText
    private lateinit var roll: EditText
    private lateinit var clazz: EditText
    private lateinit var exam: EditText
    private lateinit var session: EditText
    private lateinit var subjectBox: LinearLayout
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var save: Button
    private lateinit var summary: TextView
    private var logo: Bitmap? = null
    private var sheet: Bitmap? = null
    private var unsaved = false
    private var verificationId: String? = null
    private val db = FirebaseFirestore.getInstance()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(c: String, r: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(c)); cornerRadius = dp(r).toFloat()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(20), dp(18), dp(30)); setBackgroundColor(Color.parseColor("#F6F7FC"))
        }
        root.addView(TextView(this).apply { text = "Professional Marksheet Maker"; textSize = 26f; setTypeface(typeface, 1) })
        root.addView(TextView(this).apply { text = "Custom passing marks • grades • verified QR • live preview"; setPadding(0, dp(5), 0, dp(14)) })
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(14)); background = rounded("#FFFFFF", 18) }
        fun field(h: String) = EditText(this).apply {
            hint = h; background = rounded("#F7F8FC", 10); setPadding(dp(12), dp(10), dp(12), dp(10)); form.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        }
        school = field("School Name"); address = field("School Address / Contact / Code"); student = field("Student Name"); roll = field("Roll / Admission No."); clazz = field("Class / Section"); exam = field("Exam Name"); session = field("Academic Session")
        form.addView(Button(this).apply { text = "Choose School Logo"; isAllCaps = false; setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }, 401) } })
        root.addView(form)
        root.addView(TextView(this).apply { text = "SUBJECTS & MARKS"; setTypeface(typeface, 1); setPadding(0, dp(18), 0, dp(7)) })
        subjectBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(subjectBox)
        root.addView(Button(this).apply { text = "＋ Add Subject"; isAllCaps = false; setOnClickListener { addSubject() } })
        summary = TextView(this).apply { setPadding(dp(12), dp(12), dp(12), dp(12)); background = rounded("#FFFFFF", 12) }; root.addView(summary)
        preview = ImageView(this).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER }; root.addView(preview, LinearLayout.LayoutParams(-1, dp(430)))
        root.addView(Button(this).apply { text = "Register Verification & Finalize"; isAllCaps = false; setOnClickListener { generate() } })
        save = Button(this).apply { text = "Save Marksheet PDF"; isAllCaps = false; visibility = View.GONE; setOnClickListener { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "application/pdf"; putExtra(Intent.EXTRA_TITLE, "EduNexa_Marksheet_${verificationId}.pdf") }, 402) } }; root.addView(save)
        status = TextView(this).apply { text = "Live preview only • Nothing saved until finalization."; setPadding(0, dp(10), 0, 0) }; root.addView(status)
        setContentView(ScrollView(this).apply { addView(root) })
        val w = watcher(); listOf(school, address, student, roll, clazz, exam, session).forEach { it.addTextChangedListener(w) }
        repeat(5) { addSubject() }; updatePreview()
    }

    private fun watcher() = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { invalidateFinal(); updatePreview() }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun invalidateFinal() {
        if (::save.isInitialized && save.visibility == View.VISIBLE) { save.visibility = View.GONE; sheet = null; verificationId = null; unsaved = false; status.text = "Details changed • Finalize again." }
    }

    private fun addSubject() {
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)); background = rounded("#FFFFFF", 14) }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val name = EditText(this).apply { hint = "Subject" }
        val max = EditText(this).apply { hint = "Max"; inputType = 2 }
        val got = EditText(this).apply { hint = "Marks"; inputType = 2 }
        val pass = EditText(this).apply { hint = "Pass"; inputType = 2 }
        listOf(name, max, got, pass).forEach { it.addTextChangedListener(watcher()) }
        row.addView(name, LinearLayout.LayoutParams(0, -2, 2f)); row.addView(max, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(got, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(pass, LinearLayout.LayoutParams(0, -2, 1f)); card.addView(row)
        val optional = CheckBox(this).apply { text = "Optional subject"; setOnCheckedChangeListener { _, _ -> invalidateFinal(); updatePreview() } }; card.addView(optional)
        card.addView(Button(this).apply { text = "Remove"; isAllCaps = false; setOnClickListener { subjectBox.removeView(card); invalidateFinal(); updatePreview() } })
        card.tag = listOf<View>(name, max, got, pass, optional); subjectBox.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }

    private fun rows(): List<Subject> {
        val out = mutableListOf<Subject>()
        for (i in 0 until subjectBox.childCount) {
            val t = subjectBox.getChildAt(i).tag as? List<*> ?: continue
            val n = (t[0] as EditText).text.toString().trim(); val m = (t[1] as EditText).text.toString().toIntOrNull(); val g = (t[2] as EditText).text.toString().toIntOrNull(); val passValue = (t[3] as EditText).text.toString().toIntOrNull(); val optional = (t[4] as CheckBox).isChecked
            if (n.isNotBlank() && m != null && g != null && m > 0 && g in 0..m) out.add(Subject(n, m, g, (passValue ?: kotlin.math.ceil(m * 0.33).toInt()).coerceIn(0, m), optional))
        }
        return out
    }

    private fun calc(r: List<Subject>): Triple<Int, Int, Double> { val main = r.filter { !it.optional }; val m = main.sumOf { it.max }; val g = main.sumOf { it.got }; return Triple(m, g, if (m > 0) g * 100.0 / m else 0.0) }
    private fun result(r: List<Subject>) = if (r.filter { !it.optional }.all { it.got >= it.pass }) "PASS" else "FAIL"
    private fun updatePreview() { if (!::preview.isInitialized) return; val r = rows(); val (m, g, pct) = calc(r); val rs = if (r.none { !it.optional }) "—" else result(r); summary.text = if (m > 0) "Total: $g / $m • ${"%.2f".format(pct)}% • $rs" else "Total: — • Percentage: — • Result: —"; preview.setImageBitmap(drawSheet(r, m, g, pct, rs, false, null)) }

    private fun drawSheet(r: List<Subject>, max: Int, got: Int, pct: Double, rs: String, final: Boolean, id: String?): Bitmap {
        val width = 900; val height = maxOf(1180, 680 + r.size * 43); val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); val c = Canvas(b); c.drawColor(Color.WHITE); val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(28, 48, 94) }
        c.drawRect(0f, 0f, width.toFloat(), 14f, p); p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 38f; c.drawText(school.text.toString().ifBlank { "SCHOOL NAME" }, width / 2f, 70f, p); p.typeface = Typeface.DEFAULT; p.textSize = 16f; c.drawText(address.text.toString().ifBlank { "School address / contact" }.take(80), width / 2f, 100f, p); logo?.let { c.drawBitmap(Bitmap.createScaledBitmap(it, 75, 75, true), 30f, 28f, null) }
        p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 25f; c.drawText("ACADEMIC MARKSHEET", width / 2f, 148f, p); p.textAlign = Paint.Align.LEFT; p.typeface = Typeface.DEFAULT; p.textSize = 17f; c.drawText("Student: ${student.text.toString().ifBlank { "—" }}", 42f, 198f, p); c.drawText("Roll: ${roll.text.toString().ifBlank { "—" }}", 490f, 198f, p); c.drawText("Class: ${clazz.text.toString().ifBlank { "—" }}", 42f, 230f, p); c.drawText("Exam: ${exam.text.toString().ifBlank { "—" }}", 490f, 230f, p); c.drawText("Session: ${session.text.toString().ifBlank { "—" }}", 42f, 262f, p)
        var y = 320f; p.typeface = Typeface.DEFAULT_BOLD; c.drawText("SUBJECT", 42f, y, p); c.drawText("MAX", 450f, y, p); c.drawText("PASS", 550f, y, p); c.drawText("MARKS", 650f, y, p); c.drawText("GRADE", 775f, y, p); p.typeface = Typeface.DEFAULT
        r.forEach { s -> y += 43; c.drawText((s.name + if (s.optional) " (Optional)" else "").take(32), 42f, y, p); c.drawText(s.max.toString(), 450f, y, p); c.drawText(s.pass.toString(), 550f, y, p); c.drawText(s.got.toString(), 650f, y, p); c.drawText(grade(s.got, s.max), 775f, y, p) }
        y += 65; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 19f; c.drawText("Total: $got / $max", 42f, y, p); val percentageText = if (max > 0) String.format("%.2f%%", pct) else "—"; c.drawText("Percentage: $percentageText", 300f, y, p); c.drawText("Result: $rs", 650f, y, p)
        p.typeface = Typeface.DEFAULT; p.textSize = 14f; if (final && id != null) { val qr = qr("EDUNEXA:RESULT:$id", 180); c.drawBitmap(qr, width - 220f, height - 220f, null); c.drawText("Scan to verify", width - 215f, height - 25f, p); c.drawText("Verification ID: $id", 42f, height - 60f, p) } else c.drawText("Live preview • Not registered yet", 42f, height - 60f, p)
        return b
    }

    private fun qr(v: String, size: Int): Bitmap { val m = QRCodeWriter().encode(v, BarcodeFormat.QR_CODE, size, size); val b = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565); for (y in 0 until size) for (x in 0 until size) b.setPixel(x, y, if (m[x, y]) Color.BLACK else Color.WHITE); return b }

    private fun generate() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run { status.text = "Login required"; return }; val r = rows(); if (school.text.isBlank() || student.text.isBlank() || r.none { !it.optional }) { status.text = "Enter school, student and at least one main subject"; return }; val (m, g, pct) = calc(r); val rs = result(r); status.text = "Registering verification…"
        db.collection("users").document(uid).get().addOnSuccessListener { u ->
            val approved = u.getString("role") == "school" && u.getBoolean("schoolApproved") == true; val id = "EDX-" + UUID.randomUUID().toString().take(8).uppercase(); val record = hashMapOf<String, Any>("verificationId" to id, "issuerUid" to uid, "issuerApproved" to approved, "schoolName" to school.text.toString(), "studentName" to student.text.toString(), "rollNo" to roll.text.toString(), "className" to clazz.text.toString(), "examName" to exam.text.toString(), "session" to session.text.toString(), "subjects" to r.map { mapOf("name" to it.name, "max" to it.max, "obtained" to it.got, "passMarks" to it.pass, "optional" to it.optional) }, "totalObtained" to g, "totalMax" to m, "percentage" to pct, "result" to rs, "qrPayload" to "EDUNEXA:RESULT:$id", "createdAt" to FieldValue.serverTimestamp())
            db.collection("marksheetVerifications").document(id).set(record).addOnSuccessListener { verificationId = id; sheet = drawSheet(r, m, g, pct, rs, true, id); preview.setImageBitmap(sheet); save.visibility = View.VISIBLE; unsaved = true; status.text = "Verified • QR added • ID: $id" }.addOnFailureListener { status.text = "Verification failed: ${it.message}" }
        }.addOnFailureListener { status.text = "Account check failed: ${it.message}" }
    }

    override fun onActivityResult(rc: Int, res: Int, data: Intent?) {
        super.onActivityResult(rc, res, data); if (res != Activity.RESULT_OK) return
        if (rc == 401) { logo = data?.data?.let { contentResolver.openInputStream(it)?.use { stream -> BitmapFactory.decodeStream(stream) } }; invalidateFinal(); updatePreview() }
        else if (rc == 402) { val uri = data?.data ?: return; val b = sheet ?: return; val pdf = PdfDocument(); val page = pdf.startPage(PdfDocument.PageInfo.Builder(b.width, b.height, 1).create()); page.canvas.drawBitmap(b, 0f, 0f, null); pdf.finishPage(page); runCatching { contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }; unsaved = false; status.text = "PDF saved • ID: $verificationId" }.onFailure { status.text = "Save failed: ${it.message}" }; pdf.close() }
    }

    private fun grade(g: Int, m: Int) = when (g * 100 / m) { in 90..100 -> "A+"; in 80..89 -> "A"; in 70..79 -> "B+"; in 60..69 -> "B"; in 50..59 -> "C"; in 33..49 -> "D"; else -> "F" }

    @Suppress("DEPRECATION")
    override fun onBackPressed() { if (unsaved) AlertDialog.Builder(this).setTitle("Discard unsaved PDF?").setMessage("Verification is registered, but PDF is not saved on this device.").setPositiveButton("Discard") { _, _ -> unsaved = false; super.onBackPressed() }.setNegativeButton("Stay", null).show() else super.onBackPressed() }
}
