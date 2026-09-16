package com.edunexa.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var content: FrameLayout
    private lateinit var homeBtn: Button
    private lateinit var toolsBtn: Button
    private lateinit var connectBtn: Button
    private lateinit var profileBtn: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var profileImage: ImageView? = null
    private var currentTab = "home"

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            getSharedPreferences("edunexa", MODE_PRIVATE).edit().putString("profilePhoto", it.toString()).apply()
            profileImage?.setImageURI(it)
            auth.currentUser?.uid?.let { uid ->
                db.collection("users").document(uid).update("profilePhotoUri", it.toString())
                    .addOnSuccessListener { Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(this, "Photo saved on this phone", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun card() = MaterialCardView(this).apply {
        radius = dp(22).toFloat(); cardElevation = dp(2).toFloat(); setCardBackgroundColor(Color.WHITE)
        strokeWidth = dp(1); strokeColor = Color.parseColor("#ECEEF6"); setContentPadding(dp(18), dp(17), dp(18), dp(17))
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); setContentView(R.layout.activity_main)
        content = findViewById(R.id.content); homeBtn = findViewById(R.id.homeBtn); toolsBtn = findViewById(R.id.toolsBtn); connectBtn = findViewById(R.id.connectBtn); profileBtn = findViewById(R.id.profileBtn)
        homeBtn.setOnClickListener { showHome() }; toolsBtn.setOnClickListener { startActivity(Intent(this, ToolsActivity::class.java)) }
        connectBtn.setOnClickListener { startActivity(Intent(this, JoinSchoolActivity::class.java)) }; profileBtn.setOnClickListener { showProfile() }
        showHome()
    }

    private fun selectTab(tab: String) {
        currentTab = tab
        val active = Color.parseColor("#4338CA"); val inactive = Color.parseColor("#596174"); val activeBg = Color.parseColor("#EEF0FF"); val plain = Color.WHITE
        listOf(homeBtn to "home", toolsBtn to "tools", connectBtn to "connect", profileBtn to "profile").forEach { (b, key) ->
            b.setTextColor(if (key == tab) active else inactive); b.backgroundTintList = android.content.res.ColorStateList.valueOf(if (key == tab) activeBg else plain)
        }
    }

    private fun action(r: LinearLayout, icon: String, title: String, subtitle: String, go: () -> Unit) {
        val c = card(); val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply { text = icon; textSize = 25f; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val textBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(8), 0) }
        textBox.addView(TextView(this).apply { text = title; textSize = 17f; setTextColor(Color.parseColor("#171A2B")); setTypeface(typeface, 1) })
        textBox.addView(TextView(this).apply { text = subtitle; textSize = 13f; setTextColor(Color.parseColor("#747B91")); setPadding(0, dp(3), 0, 0) })
        row.addView(textBox, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(TextView(this).apply { text = "›"; textSize = 28f; setTextColor(Color.parseColor("#7C73E6")) })
        c.addView(row); c.setOnClickListener { go() }; r.addView(c, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(11)) })
    }

    private fun showHome() {
        selectTab("home"); content.removeAllViews()
        val r = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(3), dp(2), dp(3), dp(24)) }
        val welcome = card(); val wb = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val hello = TextView(this).apply { text = "Welcome back 👋"; textSize = 14f; setTextColor(Color.parseColor("#6B7280")) }
        val name = TextView(this).apply { text = "Student"; textSize = 25f; setTypeface(typeface, 1); setTextColor(Color.parseColor("#171A2B")) }
        val school = TextView(this).apply { text = "Your learning dashboard"; textSize = 13f; setTextColor(Color.parseColor("#747B91")); setPadding(0, dp(4), 0, 0) }
        wb.addView(hello); wb.addView(name); wb.addView(school); welcome.addView(wb); r.addView(welcome, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(16)) })
        auth.currentUser?.uid?.let { uid -> db.collection("users").document(uid).get().addOnSuccessListener { d -> name.text = d.getString("name") ?: "Student"; val schoolName=d.getString("schoolName"); val className=d.getString("className"); school.text=listOfNotNull(className?.let{"Class $it"},schoolName).joinToString(" • ").ifBlank{"Your learning dashboard"} } }
        r.addView(TextView(this).apply { text = "Quick Actions"; textSize = 19f; setTypeface(typeface, 1); setPadding(dp(3), dp(3), 0, dp(10)) })
        action(r, "🧮", "Result Calculator", "Percentage, grade and result history") { startActivity(Intent(this, ResultCalculatorActivity::class.java)) }
        action(r, "📝", "MCQ Tests", "Take tests assigned by your school") { startActivity(Intent(this, StudentMcqActivity::class.java)) }
        action(r, "📊", "School Results", "View results published by your school") { startActivity(Intent(this, StudentResultsActivity::class.java)) }
        action(r, "📅", "My Attendance", "Check your attendance record") { startActivity(Intent(this, StudentAttendanceActivity::class.java)) }
        action(r, "📢", "Notices", "School announcements and updates") { startActivity(Intent(this, NoticeActivity::class.java)) }
        action(r, "🏫", "Connect School", "Join securely using your school code") { startActivity(Intent(this, JoinSchoolActivity::class.java)) }
        action(r, "🛠", "EduNexa Tools", "Image, PDF, marksheet and utility tools") { startActivity(Intent(this, ToolsActivity::class.java)) }
        content.addView(ScrollView(this).apply { addView(r) })
    }

    private fun showProfile() {
        selectTab("profile"); content.removeAllViews()
        val r = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(4), dp(4), dp(4), dp(28)) }
        val profileCard = card(); val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        box.addView(TextView(this).apply { text = "My Profile"; textSize = 24f; setTypeface(typeface, 1); setTextColor(Color.parseColor("#171A2B")) })
        profileImage = ImageView(this).apply { setImageResource(R.drawable.edunexa_logo); scaleType = ImageView.ScaleType.CENTER_CROP }
        box.addView(profileImage, LinearLayout.LayoutParams(dp(112), dp(112)).apply { setMargins(0, dp(18), 0, dp(8)) })
        val localPhoto = getSharedPreferences("edunexa", MODE_PRIVATE).getString("profilePhoto", null); localPhoto?.let { runCatching { profileImage?.setImageURI(Uri.parse(it)) } }
        box.addView(Button(this).apply { text = "Change Profile Photo"; isAllCaps = false; setOnClickListener { photoPicker.launch(arrayOf("image/*")) } })
        val info = TextView(this).apply { text = "Loading account…"; textSize = 16f; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#454B60")); setPadding(0, dp(18), 0, dp(8)) }; box.addView(info)
        val badge = TextView(this).apply { text = "Student Account"; textSize = 13f; setTextColor(Color.parseColor("#4338CA")); setBackgroundColor(Color.parseColor("#EEF0FF")); setPadding(dp(14), dp(7), dp(14), dp(7)) }; box.addView(badge)
        profileCard.addView(box); r.addView(profileCard, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) })
        auth.currentUser?.uid?.let { uid -> db.collection("users").document(uid).get().addOnSuccessListener { d ->
            val displayName=d.getString("name")?:"Student"; val email=d.getString("email")?:auth.currentUser?.email.orEmpty(); val cls=d.getString("className")?:"—"; val school=d.getString("schoolName")
            info.text="$displayName\n$email\nClass: $cls${school?.let{"\n$it"}?:""}"
            if(localPhoto==null) d.getString("profilePhotoUri")?.let { runCatching { profileImage?.setImageURI(Uri.parse(it)) } }
        }.addOnFailureListener { info.text="Account details unavailable\nCheck your internet connection." } }
        action(r, "🏆", "MCQ History", "Completed tests and scores") { startActivity(Intent(this, McqHistoryActivity::class.java)) }
        action(r, "🛠", "EduNexa Tools", "Open all student utility tools") { startActivity(Intent(this, ToolsActivity::class.java)) }
        r.addView(Button(this).apply { text = "Logout"; isAllCaps = false; setTextColor(Color.parseColor("#B42318")); setOnClickListener { auth.signOut(); getSharedPreferences("edunexa",MODE_PRIVATE).edit().remove("profilePhoto").apply(); startActivity(Intent(this@MainActivity, AuthActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }); finish() } }, LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0, dp(8), 0, 0) })
        content.addView(ScrollView(this).apply { addView(r) })
    }
}
