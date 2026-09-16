package com.edunexa.app

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var content: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        content = findViewById(R.id.content)
        findViewById<Button>(R.id.homeBtn).setOnClickListener { showHome() }
        findViewById<Button>(R.id.toolsBtn).setOnClickListener { showMessage("Student Tools", "Image tools and utilities will live here.") }
        findViewById<Button>(R.id.connectBtn).setOnClickListener { showMessage("Connect", "Students and approved schools will connect here.") }
        findViewById<Button>(R.id.profileBtn).setOnClickListener { showMessage("Profile", "Student profile, history and account settings.") }
        showHome()
    }

    private fun showHome() {
        content.removeAllViews()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val title = TextView(this).apply { text = "Result Calculator"; textSize = 24f }
        val obtained = EditText(this).apply {
            hint = "Obtained marks"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val total = EditText(this).apply {
            hint = "Total marks"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val result = TextView(this).apply { textSize = 22f; gravity = Gravity.CENTER; setPadding(0, 24, 0, 0) }
        val calculate = Button(this).apply {
            text = "Calculate Result"
            setOnClickListener {
                val a = obtained.text.toString().toDoubleOrNull()
                val b = total.text.toString().toDoubleOrNull()
                result.text = if (a == null || b == null || b <= 0 || a < 0 || a > b) {
                    "Enter valid marks"
                } else {
                    val p = a / b * 100
                    val grade = when {
                        p >= 90 -> "A+"
                        p >= 80 -> "A"
                        p >= 70 -> "B"
                        p >= 60 -> "C"
                        p >= 45 -> "D"
                        p >= 33 -> "E"
                        else -> "F"
                    }
                    "%.2f%%  •  Grade %s".format(p, grade)
                }
            }
        }
        box.addView(title); box.addView(obtained); box.addView(total); box.addView(calculate); box.addView(result)
        content.addView(box)
    }

    private fun showMessage(titleText: String, bodyText: String) {
        content.removeAllViews()
        val text = TextView(this).apply {
            text = "$titleText\n\n$bodyText"
            textSize = 20f
            gravity = Gravity.CENTER
        }
        content.addView(text)
    }
}
