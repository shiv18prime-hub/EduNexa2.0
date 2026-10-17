package com.edunexa.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.edunexa.app.data.SchoolScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeacherActivity : EduActivity() {
    private lateinit var status: TextView
    private lateinit var greeting: TextView
    private val schoolActions = mutableListOf<MaterialButton>()
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val ui = StudyUi(this)
        val root = ui.screen("Teacher workspace", "Everything you need for a productive school day.")
        val hero = ui.panel(root)
        greeting = ui.text("Welcome back", 24f, true); hero.addView(greeting)
        status = ui.text("Checking your school connection…", 14f, color = "#6258C7"); hero.addView(status)
        fun open(target: Class<*>) { startActivity(Intent(this, target)) }
        ui.button(hero, "Manage school connection") { open(JoinSchoolActivity::class.java) }
        root.addView(ui.text("Your classroom", 21f, true))
        val classroom = ui.panel(root)
        classroom.addView(ui.text("Publish and keep everyone informed", 15f, color = "#667085"))
        listOf("Publish student results" to PublishResultActivity::class.java,
            "Mark attendance" to AttendanceActivity::class.java,
            "Publish a notice" to PublishNoticeActivity::class.java,
            "School messages" to ChatActivity::class.java).forEach { (label, target) ->
            schoolActions.add(ui.button(classroom, label) { open(target) }.apply { isEnabled = false })
        }
        if (BuildConfig.HOSTED_SERVICES_ENABLED) schoolActions.add(ui.button(classroom, "Publish school test") { open(PublishMcqActivity::class.java) }.apply { isEnabled = false })
        root.addView(ui.text("Prepare & practise", 21f, true))
        ui.action(root, "STUDY DESK", "Quizzes, reports & tools", "Offline practice, result calculation and file utilities") { open(StudyDeskActivity::class.java) }
        ui.button(root, "My profile") { startActivity(Intent(this, MainActivity::class.java).putExtra("profileOnly", true)) }
        ui.button(root, "Sign out") { Session.logout(this) }
    }
    override fun onResume() {
        super.onResume()
        schoolActions.forEach { it.isEnabled = false }
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return finish()
        FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener { profile ->
            if (profile.getString("role") != "teacher") { finish(); return@addOnSuccessListener }
            greeting.text = profile.getString("name") ?: "Welcome back"
            SchoolScope.resolve({
                status.text = "Connected to ${profile.getString("schoolName").orEmpty().ifBlank { "your school" }}"
                schoolActions.forEach { it.isEnabled = true }
            }, { status.text = it })
        }.addOnFailureListener { status.text = "Could not load your school. Offline study tools are available below." }
    }
}
