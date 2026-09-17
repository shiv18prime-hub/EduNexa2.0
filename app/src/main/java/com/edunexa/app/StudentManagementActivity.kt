package com.edunexa.app

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class StudentManagementActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)) }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(TextView(this).apply { text = "Student Management"; textSize = 27f; setTypeface(typeface, 1) })
        status = TextView(this).apply { text = "Loading students and requests…"; setPadding(0, dp(8), 0, dp(14)) }; root.addView(status)
        load()
    }

    private fun card(title: String, body: String, buttons: List<Pair<String, () -> Unit>> = emptyList()) {
        val c = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = dp(2).toFloat(); setCardBackgroundColor(Color.WHITE); setContentPadding(dp(16), dp(16), dp(16), dp(16)) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(TextView(this).apply { text = title; textSize = 17f; setTypeface(typeface, 1); setTextColor(Color.parseColor("#20243A")) })
        box.addView(TextView(this).apply { text = body; textSize = 14f; setTextColor(Color.parseColor("#646B80")); setPadding(0, dp(5), 0, if (buttons.isEmpty()) 0 else dp(8)) })
        buttons.forEach { (label, action) -> box.addView(Button(this).apply { text = label; isAllCaps = false; setOnClickListener { isEnabled = false; action() } }, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, dp(4), 0, 0) }) }
        c.addView(box); root.addView(c, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(10)) })
    }

    private fun load() {
        val sid = auth.currentUser?.uid ?: return
        while (root.childCount > 2) root.removeViewAt(2)
        status.text = "Loading students and requests…"
        db.collection("schoolJoinRequests").whereEqualTo("schoolId", sid).whereEqualTo("status", "pending").get().addOnSuccessListener { requests ->
            if (!requests.isEmpty) root.addView(TextView(this).apply { text = "Pending Requests (${requests.size()})"; textSize = 19f; setTypeface(typeface, 1); setPadding(0, dp(5), 0, dp(8)) }, 2)
            requests.documents.forEach { r ->
                val studentId = r.getString("studentId").orEmpty(); val code = r.getString("schoolCode").orEmpty()
                card(r.getString("studentName") ?: "Student", "${r.getString("studentEmail") ?: ""}\nClass: ${r.getString("className") ?: "—"}", listOf(
                    "✓ Approve Student" to { approve(r.id, studentId, code, sid) },
                    "Reject Request" to { reject(r.id, studentId) }
                ))
            }
            loadConnected(sid, requests.size())
        }.addOnFailureListener { status.text = it.message ?: "Could not load requests"; loadConnected(sid, 0) }
    }

    private fun loadConnected(sid: String, pending: Int) {
        db.collection("users").whereEqualTo("role", "student").whereEqualTo("schoolId", sid).get().addOnSuccessListener { students ->
            status.text = "${students.size()} connected • $pending pending"
            root.addView(TextView(this).apply { text = "Connected Students (${students.size()})"; textSize = 19f; setTypeface(typeface, 1); setPadding(0, dp(10), 0, dp(8)) })
            students.documents.forEach { d -> card(d.getString("name") ?: "Student", "${d.getString("email") ?: ""}\nClass: ${d.getString("className") ?: "Not set"}\nSchool Code: ${d.getString("schoolCode") ?: "—"}") }
        }.addOnFailureListener { status.text = it.message ?: "Could not load connected students" }
    }

    private fun approve(requestId: String, studentId: String, code: String, schoolId: String) {
        if (studentId.isBlank()) return reload("Invalid student request")
        db.collection("users").document(studentId).update(
            "schoolId", schoolId, "schoolCode", code, "schoolJoinStatus", "connected", "schoolJoinedAt", FieldValue.serverTimestamp(),
            "pendingSchoolId", FieldValue.delete(), "pendingSchoolCode", FieldValue.delete()
        ).addOnSuccessListener {
            db.collection("schoolJoinRequests").document(requestId).update("status", "approved", "reviewedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener { reload("Student approved") }.addOnFailureListener { reload("Student connected; request status refresh failed") }
        }.addOnFailureListener { reload(it.message ?: "Could not approve student") }
    }

    private fun reject(requestId: String, studentId: String) {
        db.collection("schoolJoinRequests").document(requestId).update("status", "rejected", "reviewedAt", FieldValue.serverTimestamp()).addOnSuccessListener {
            if (studentId.isNotBlank()) db.collection("users").document(studentId).update("schoolJoinStatus", "rejected", "pendingSchoolId", FieldValue.delete(), "pendingSchoolCode", FieldValue.delete())
            reload("Request rejected")
        }.addOnFailureListener { reload(it.message ?: "Could not reject request") }
    }

    private fun reload(message: String) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); load() }
}
