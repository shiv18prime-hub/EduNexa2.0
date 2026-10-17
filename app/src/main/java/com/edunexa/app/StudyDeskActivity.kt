package com.edunexa.app

import android.content.Intent
import android.os.Bundle

class StudyDeskActivity : EduActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val ui = StudyUi(this)
        val root = ui.screen("Your study desk", "Practice, calculate and create — even offline.") { finish() }
        val welcome = ui.panel(root)
        welcome.addView(ui.text("READY WHEN YOU ARE", 11f, true, "#6258C7"))
        welcome.addView(ui.text("Small steps. Real progress.", 23f, true))
        welcome.addView(ui.text("Use these tools without an account. Sign in from the welcome screen to connect with your school and sync your profile.", 14f, color = "#667085"))
        fun open(target: Class<*>) { startActivity(Intent(this, target)) }
        ui.action(root, "PRACTICE", "Quick quizzes", "Computers, maths and English • answers explained") { open(PracticeActivity::class.java) }
        ui.action(root, "CALCULATE", "Result calculator", "Custom subjects, passing marks and shareable results") { open(ResultCalculatorActivity::class.java) }
        ui.action(root, "CREATE", "PDF study report", "Preview and export a personal practice report") { open(MarksheetMakerActivity::class.java) }
        ui.action(root, "TOOLS", "Images & documents", "Resize, compress, combine and prepare PDFs") { open(ToolsActivity::class.java) }
        root.addView(ui.text("EduNexa ${BuildConfig.VERSION_NAME} • Offline practice stays on this device.", 12f, color = "#667085"))
    }
}
