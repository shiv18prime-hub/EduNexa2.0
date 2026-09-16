package com.edunexa.app

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    private lateinit var content: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        content = findViewById(R.id.content)
        findViewById<Button>(R.id.homeBtn).setOnClickListener { showHome() }
        findViewById<Button>(R.id.toolsBtn).setOnClickListener { showTools() }
        findViewById<Button>(R.id.connectBtn).setOnClickListener { showMessage("Connect", "Join your verified school with School Code\n\nSchool announcements, results and attendance will appear here.") }
        findViewById<Button>(R.id.profileBtn).setOnClickListener { showMessage("My Profile", "Account • Result History • Pro Status • Settings") }
        showHome()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun card(): MaterialCardView = MaterialCardView(this).apply {
        radius = dp(22).toFloat()
        cardElevation = dp(3).toFloat()
        setCardBackgroundColor(Color.WHITE)
        setContentPadding(dp(18), dp(18), dp(18), dp(18))
    }

    private fun showHome() {
        content.removeAllViews()
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(TextView(this).apply { text = "Hi, Student 👋"; textSize = 25f; setTextColor(Color.rgb(25, 31, 55)); setTypeface(typeface, 1) })
        root.addView(TextView(this).apply { text = "Ready to calculate your result?"; textSize = 14f; setTextColor(Color.rgb(105, 112, 132)); setPadding(0, dp(4), 0, dp(16)) })

        val calculator = card()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(TextView(this).apply { text = "Result Calculator"; textSize = 21f; setTypeface(typeface, 1); setTextColor(Color.rgb(42, 45, 67)) })
        box.addView(TextView(this).apply { text = "Quick percentage & grade"; setTextColor(Color.GRAY); setPadding(0, 0, 0, dp(10)) })
        val obtained = EditText(this).apply { hint = "Obtained marks"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val total = EditText(this).apply { hint = "Total marks"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val result = TextView(this).apply { textSize = 24f; gravity = Gravity.CENTER; setPadding(0, dp(18), 0, 0); setTextColor(Color.rgb(67, 56, 202)); setTypeface(typeface, 1) }
        val calculate = Button(this).apply {
            text = "Calculate Result"; isAllCaps = false
            setOnClickListener {
                val a = obtained.text.toString().toDoubleOrNull(); val b = total.text.toString().toDoubleOrNull()
                result.text = if (a == null || b == null || b <= 0 || a < 0 || a > b) "Enter valid marks" else {
                    val p = a / b * 100
                    val grade = when { p >= 90 -> "A+"; p >= 80 -> "A"; p >= 70 -> "B"; p >= 60 -> "C"; p >= 45 -> "D"; p >= 33 -> "E"; else -> "F" }
                    "%.2f%%   •   Grade %s".format(p, grade)
                }
            }
        }
        box.addView(obtained); box.addView(total); box.addView(calculate); box.addView(result)
        calculator.addView(box); root.addView(calculator)
        root.addView(TextView(this).apply { text = "Quick Access"; textSize = 19f; setTypeface(typeface, 1); setPadding(0, dp(22), 0, dp(10)) })
        root.addView(TextView(this).apply { text = "🖼 Image Tools     📄 PDF Tools\n\n🏫 Join School      🕘 Result History"; textSize = 17f; setPadding(dp(10), dp(8), dp(10), dp(20)); setTextColor(Color.rgb(55, 60, 80)) })
        scroll.addView(root); content.addView(scroll)
    }

    private fun showTools() {
        content.removeAllViews()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(TextView(this).apply { text = "Student Tools"; textSize = 25f; setTypeface(typeface, 1); setTextColor(Color.rgb(25,31,55)) })
        root.addView(TextView(this).apply { text = "Fast utilities for study and documents"; setTextColor(Color.GRAY); setPadding(0, dp(4), 0, dp(18)) })
        val tools = arrayOf("Image Resize", "Image Compress", "Image Merge", "Background Remover", "PDF Tools", "Image ↔ PDF")
        tools.forEach { name ->
            val c = card(); val t = TextView(this).apply { text = name; textSize = 17f; setTypeface(typeface, 1); setTextColor(Color.rgb(50,55,80)) }
            c.addView(t); val lp = LinearLayout.LayoutParams(-1, dp(68)); lp.setMargins(0, 0, 0, dp(10)); root.addView(c, lp)
        }
        content.addView(ScrollView(this).apply { addView(root) })
    }

    private fun showMessage(titleText: String, bodyText: String) {
        content.removeAllViews()
        val c = card(); c.addView(TextView(this).apply { text = "$titleText\n\n$bodyText"; textSize = 19f; setTextColor(Color.rgb(45,50,75)); gravity = Gravity.CENTER; setPadding(dp(8), dp(16), dp(8), dp(16)) })
        content.addView(c)
    }
}
