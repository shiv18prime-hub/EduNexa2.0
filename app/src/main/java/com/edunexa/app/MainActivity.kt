package com.edunexa.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : EduActivity() {
    private lateinit var content: FrameLayout
    private lateinit var homeBtn: Button
    private lateinit var toolsBtn: Button
    private lateinit var connectBtn: Button
    private lateinit var profileBtn: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var profileImage: ImageView? = null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if(uri!=null){ Toast.makeText(this,"Uploading profile photo…",Toast.LENGTH_SHORT).show()
            com.edunexa.app.data.ProfilePhotos.upload(this,uri,{url->profileImage?.let{com.edunexa.app.data.ProfilePhotos.load(it,url)};Toast.makeText(this,"Photo saved and synced",Toast.LENGTH_SHORT).show()},{error->Toast.makeText(this,error,Toast.LENGTH_LONG).show()})
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun card(radius: Int = 22) = MaterialCardView(this).apply {
        this.radius = dp(radius).toFloat(); cardElevation = dp(2).toFloat(); setCardBackgroundColor(Color.WHITE)
        strokeWidth = dp(1); strokeColor = Color.parseColor("#E8EAF3"); setContentPadding(dp(18), dp(17), dp(18), dp(17))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main)
        content = findViewById(R.id.content); homeBtn = findViewById(R.id.homeBtn); toolsBtn = findViewById(R.id.toolsBtn); connectBtn = findViewById(R.id.connectBtn); profileBtn = findViewById(R.id.profileBtn)
        homeBtn.setOnClickListener { showHome() }; toolsBtn.setOnClickListener { startActivity(Intent(this, ToolsActivity::class.java)) }
        connectBtn.setOnClickListener { startActivity(Intent(this, JoinSchoolActivity::class.java)) }; profileBtn.setOnClickListener { showProfile() }
        if(intent.getBooleanExtra("profileOnly",false))showProfile() else showHome()
    }

    private fun selectTab(tab: String) {
        val active = Color.parseColor("#4338CA"); val inactive = Color.parseColor("#596174"); val activeBg = Color.parseColor("#EEF0FF")
        listOf(homeBtn to "home", toolsBtn to "tools", connectBtn to "connect", profileBtn to "profile").forEach { (button, key) ->
            button.setTextColor(if (key == tab) active else inactive); button.backgroundTintList = android.content.res.ColorStateList.valueOf(if (key == tab) activeBg else Color.WHITE)
        }
    }

    private fun sectionTitle(text: String) = TextView(this).apply { this.text = text; textSize = 19f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(Color.parseColor("#171A2B")); setPadding(dp(3), dp(5), 0, dp(10)) }

    private fun action(root: LinearLayout, icon: String, title: String, subtitle: String, go: () -> Unit) {
        val c = card(); val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply { text = icon; textSize = 25f; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val textBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(8), 0) }
        textBox.addView(TextView(this).apply { text = title; textSize = 17f; setTextColor(Color.parseColor("#171A2B")); setTypeface(typeface, android.graphics.Typeface.BOLD) })
        textBox.addView(TextView(this).apply { text = subtitle; textSize = 13f; setTextColor(Color.parseColor("#747B91")); setPadding(0, dp(3), 0, 0) })
        row.addView(textBox, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(TextView(this).apply { text = "›"; textSize = 28f; setTextColor(Color.parseColor("#7C73E6")) })
        c.addView(row); c.setOnClickListener { go() }; root.addView(c, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(11)) })
    }

    private data class HomeAction(val icon: String, val title: String, val subtitle: String, val open: () -> Unit)

    private fun homeTile(item: HomeAction): MaterialCardView {
        val tile = card(20).apply { setContentPadding(dp(14), dp(14), dp(14), dp(14)); cardElevation = dp(1).toFloat() }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(TextView(this).apply {
            text = item.icon; textSize = 23f; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F0F1FF")); setTextColor(Color.parseColor("#4338CA"))
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        box.addView(TextView(this).apply {
            text = item.title; textSize = 15f; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(Color.parseColor("#17213D")); setPadding(0, dp(11), 0, 0)
        })
        box.addView(TextView(this).apply {
            text = item.subtitle; textSize = 11f; maxLines = 2; setLineSpacing(0f, 1.05f)
            setTextColor(Color.parseColor("#747B91")); setPadding(0, dp(4), 0, 0)
        })
        tile.addView(box)
        tile.setOnClickListener { item.open() }
        return tile
    }

    private fun homeGrid(root: LinearLayout, items: List<HomeAction>) {
        items.chunked(2).forEach { rowItems ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowItems.forEachIndexed { index, item ->
                row.addView(homeTile(item), LinearLayout.LayoutParams(0, dp(126), 1f).apply {
                    if (index == 0) setMargins(0, 0, dp(6), dp(10)) else setMargins(dp(6), 0, 0, dp(10))
                })
            }
            if (rowItems.size == 1) row.addView(android.view.View(this), LinearLayout.LayoutParams(0, dp(126), 1f).apply { setMargins(dp(6), 0, 0, dp(10)) })
            root.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun statCard(title: String, initial: String): Pair<MaterialCardView, TextView> {
        val c = card(18); val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        val value = TextView(this).apply { text = initial; textSize = 21f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(Color.parseColor("#4338CA")); gravity = Gravity.CENTER }
        box.addView(value); box.addView(TextView(this).apply { text = title; textSize = 12f; setTextColor(Color.parseColor("#747B91")); gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) }); c.addView(box)
        return c to value
    }

    private fun showHome() {
        selectTab("home"); content.removeAllViews(); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(3), dp(2), dp(3), dp(28)) }
        val hero = card(24).apply { setCardBackgroundColor(Color.parseColor("#F7F7FF")); strokeColor = Color.parseColor("#DDDDFB"); setContentPadding(dp(20), dp(18), dp(20), dp(18)) }; val heroBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val hello = TextView(this).apply { text = "YOUR LEARNING SPACE"; textSize = 10f; letterSpacing = .12f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(Color.parseColor("#6258C7")) }
        val name = TextView(this).apply { text = "Welcome, Student"; textSize = 25f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(Color.parseColor("#171A2B")); setPadding(0, dp(4), 0, 0) }
        val identity = TextView(this).apply { text = "A smarter way to learn and grow"; textSize = 13f; setTextColor(Color.parseColor("#666D82")); setPadding(0, dp(4), 0, dp(12)) }
        val status = TextView(this).apply { text = "●  Checking your school connection"; textSize = 12f; setTextColor(Color.parseColor("#4338CA")); setPadding(dp(11), dp(7), dp(11), dp(7)); setBackgroundColor(Color.parseColor("#ECECFF")) }
        heroBox.addView(hello); heroBox.addView(name); heroBox.addView(identity); heroBox.addView(status); hero.addView(heroBox); root.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(15)) })
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; val (resultCard, resultValue) = statCard("Latest Result", "—"); val (attendanceCard, attendanceValue) = statCard("Attendance", "—")
        stats.addView(resultCard, LinearLayout.LayoutParams(0, dp(92), 1f).apply { setMargins(0, 0, dp(6), 0) }); stats.addView(attendanceCard, LinearLayout.LayoutParams(0, dp(92), 1f).apply { setMargins(dp(6), 0, 0, 0) })
        resultCard.setOnClickListener { startActivity(Intent(this, StudentResultsActivity::class.java)) }; attendanceCard.setOnClickListener { startActivity(Intent(this, StudentAttendanceActivity::class.java)) }; root.addView(stats, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(16)) })
        root.addView(sectionTitle("Your learning")); homeGrid(root, listOf(
            HomeAction("🧮", "Result calculator", "Marks, grade & result") { startActivity(Intent(this, ResultCalculatorActivity::class.java)) },
            HomeAction("📝", "MCQ tests", "Practice and check scores") { startActivity(Intent(this, StudentMcqActivity::class.java)) },
            HomeAction("📊", "School results", "Published academic results") { startActivity(Intent(this, StudentResultsActivity::class.java)) },
            HomeAction("📅", "Attendance", "Review your attendance") { startActivity(Intent(this, StudentAttendanceActivity::class.java)) }
        ))
        root.addView(sectionTitle("School & tools")); homeGrid(root, listOf(
            HomeAction("📢", "Notices", "School news and updates") { startActivity(Intent(this, NoticeActivity::class.java)) },
            HomeAction("🏫", "Connect school", "Join your school securely") { startActivity(Intent(this, JoinSchoolActivity::class.java)) },
            HomeAction("💬", "Messages", "Connect with your school") { startActivity(Intent(this, ChatActivity::class.java)) },
            HomeAction("🛠", "Smart tools", "Image, PDF and more") { startActivity(Intent(this, ToolsActivity::class.java)) },
            HomeAction("⭐", "EduNexa Pro", "Membership and offers") { startActivity(Intent(this, MembershipActivity::class.java)) }
        ))
        auth.currentUser?.uid?.let { uid -> db.collection("users").document(uid).get().addOnSuccessListener { d ->
            name.text = d.getString("name") ?: auth.currentUser?.displayName ?: "Student"; val className = d.getString("className"); val schoolName = d.getString("schoolName")
            identity.text = listOfNotNull(className?.takeIf { it.isNotBlank() }?.let { "Class $it" }, schoolName?.takeIf { it.isNotBlank() }).joinToString(" • ").ifBlank { "Your personalized learning dashboard" }
            status.text = if (!schoolName.isNullOrBlank()) "●  Connected to $schoolName" else "○  School not connected • Tap Connect School"; status.setTextColor(Color.parseColor(if (!schoolName.isNullOrBlank()) "#157347" else "#B45309"))
            com.edunexa.app.data.DashboardRepository().load({stats->
                resultValue.text=(stats["latestPercentage"] as? Number)?.toDouble()?.let{"%.1f%%".format(it)}?:"—"
                attendanceValue.text=(stats["attendancePercentage"] as? Number)?.toDouble()?.let{"%.0f%%".format(it)}?:"—"
                status.text=if(stats["schoolJoinStatus"]=="connected")"● Connected to ${stats["schoolName"]}" else if(stats["schoolJoinStatus"]=="pending")"● School connection pending" else "○ School not connected"
            },{resultValue.text="View";attendanceValue.text="View"})
        }.addOnFailureListener { status.text = "Offline • Some dashboard data may be unavailable"; status.setTextColor(Color.parseColor("#B45309")); resultValue.text = "View"; attendanceValue.text = "View" } }
        content.addView(ScrollView(this).apply { addView(root) })
    }

    private fun editProfile(uid: String, currentName: String, currentClass: String, currentPhone: String, currentDob: String, currentGender: String, role: String = "student") {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(8), dp(18), 0) }
        fun field(hintText: String, value: String, numeric: Boolean = false) = EditText(this).apply { hint = hintText; setText(value); setPadding(dp(12), dp(10), dp(12), dp(10)); if (numeric) inputType = android.text.InputType.TYPE_CLASS_PHONE }.also { form.addView(it, LinearLayout.LayoutParams(-1, dp(58)).apply { setMargins(0, dp(5), 0, dp(5)) }) }
        val name = field("Full name", currentName); val cls = field("Class", currentClass); val phone = field("Phone number", currentPhone, true); val dob = field("Date of birth (DD/MM/YYYY)", currentDob); val gender = field("Gender", currentGender)
        val note = TextView(this).apply { text = "School/verified details are controlled separately and cannot be changed here."; textSize = 12f; setTextColor(Color.parseColor("#747B91")); setPadding(dp(4), dp(10), dp(4), 0) }; form.addView(note)
        val dialog = AlertDialog.Builder(this).setTitle("Edit Student Profile").setView(form).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = name.text.toString().trim(); val newClass = cls.text.toString().trim(); val newPhone = phone.text.toString().trim(); val newDob = dob.text.toString().trim(); val newGender = gender.text.toString().trim()
                if (newName.length < 2) { name.error = "Enter your full name"; return@setOnClickListener }
                if (newClass.isBlank() && role == "student") { cls.error = "Enter class"; return@setOnClickListener }
                if (newPhone.isNotBlank() && newPhone.length !in 10..15) { phone.error = "Enter a valid phone number"; return@setOnClickListener }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                val updates = hashMapOf<String, Any>("name" to newName, "className" to newClass, "phone" to newPhone, "dateOfBirth" to newDob, "gender" to newGender, "profileUpdatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
                db.collection("users").document(uid).update(updates).addOnSuccessListener { Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show(); dialog.dismiss(); showProfile() }.addOnFailureListener { e -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true; Toast.makeText(this, "Could not update profile: ${e.localizedMessage ?: "Try again"}", Toast.LENGTH_LONG).show() }
            }
        }
        dialog.show()
    }

    private fun showProfile() {
        selectTab("profile"); content.removeAllViews(); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(4), dp(4), dp(4), dp(28)) }
        val profileCard = card(); val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        box.addView(TextView(this).apply { text = "My Profile"; textSize = 24f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(Color.parseColor("#171A2B")) })
        profileImage = ImageView(this).apply { setImageResource(R.drawable.edunexa_logo); scaleType = ImageView.ScaleType.CENTER_CROP }; box.addView(profileImage, LinearLayout.LayoutParams(dp(112), dp(112)).apply { setMargins(0, dp(18), 0, dp(8)) })

        box.addView(Button(this).apply { text = "Change Profile Photo"; isAllCaps = false; setOnClickListener { photoPicker.launch(arrayOf("image/*")) } })
        val info = TextView(this).apply { text = "Loading account…"; textSize = 16f; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#454B60")); setPadding(0, dp(18), 0, dp(8)) }; box.addView(info)
        box.addView(TextView(this).apply { text = "Account Profile"; textSize = 13f; setTextColor(Color.parseColor("#4338CA")); setBackgroundColor(Color.parseColor("#EEF0FF")); setPadding(dp(14), dp(7), dp(14), dp(7)) })
        val editBtn = Button(this).apply { text = "✏️  Edit Profile"; isAllCaps = false; isEnabled = false }; box.addView(editBtn, LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0, dp(16), 0, 0) })
        profileCard.addView(box); root.addView(profileCard, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) })
        auth.currentUser?.uid?.let { uid -> db.collection("users").document(uid).get().addOnSuccessListener { d ->
            val displayName = d.getString("name") ?: "Student"; val email = d.getString("email") ?: auth.currentUser?.email.orEmpty(); val cls = d.getString("className") ?: "—"; val school = d.getString("schoolName"); val phone = d.getString("phone").orEmpty(); val dob = d.getString("dateOfBirth").orEmpty(); val gender = d.getString("gender").orEmpty()
            val extra = listOfNotNull(phone.takeIf { it.isNotBlank() }?.let { "Phone: $it" }, dob.takeIf { it.isNotBlank() }?.let { "DOB: $it" }, gender.takeIf { it.isNotBlank() }?.let { "Gender: $it" }).joinToString("\n")
            info.text = "$displayName\n$email\nClass: $cls${school?.let { "\n$it" } ?: ""}${if (extra.isNotBlank()) "\n$extra" else ""}"
            (d.getString("profilePhotoData")?:d.getString("profilePhotoUrl"))?.let { url -> profileImage?.let { com.edunexa.app.data.ProfilePhotos.load(it,url) } }
            editBtn.isEnabled = true; editBtn.setOnClickListener { editProfile(uid, displayName, if (cls == "—") "" else cls, phone, dob, gender,d.getString("role") ?: "student") }
        }.addOnFailureListener { info.text = "Account details unavailable\nCheck your internet connection." } }
        action(root, "🏆", "MCQ History", "Completed tests and scores") { startActivity(Intent(this, McqHistoryActivity::class.java)) }; action(root, "🛠", "EduNexa Tools", "Open all student utility tools") { startActivity(Intent(this, ToolsActivity::class.java)) }
        root.addView(Button(this).apply { text = "Logout"; isAllCaps = false; setTextColor(Color.parseColor("#B42318")); setOnClickListener { Session.logout(this@MainActivity) } }, LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0, dp(8), 0, 0) })
        content.addView(ScrollView(this).apply { addView(root) })
    }
}
