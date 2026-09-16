package com.edunexa.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ToolsActivity : AppCompatActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(color: String, radius: Float, stroke: String? = null): GradientDrawable = GradientDrawable().apply {
        setColor(Color.parseColor(color)); cornerRadius = dp(radius.toInt()).toFloat()
        if (stroke != null) setStroke(dp(1), Color.parseColor(stroke))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#F6F7FC")
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(22), dp(18), dp(28)); setBackgroundColor(Color.parseColor("#F6F7FC")) }
        root.addView(TextView(this).apply { text = "EduNexa Tools"; textSize = 30f; setTextColor(Color.parseColor("#17213D")); setTypeface(typeface, Typeface.BOLD) })
        root.addView(TextView(this).apply { text = "Create, convert and manage study files"; textSize = 14f; setTextColor(Color.parseColor("#72798F")); setPadding(0, dp(5), 0, dp(18)) })

        fun section(title: String, subtitle: String) {
            root.addView(TextView(this).apply { text = title; textSize = 19f; setTextColor(Color.parseColor("#17213D")); setTypeface(typeface, Typeface.BOLD); setPadding(dp(2), dp(16), 0, dp(2)) })
            root.addView(TextView(this).apply { text = subtitle; textSize = 12f; setTextColor(Color.parseColor("#8A91A5")); setPadding(dp(2), 0, 0, dp(8)) })
        }
        fun card(icon: String, title: String, detail: String, cls: Class<*>) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(15), dp(13), dp(12), dp(13)); background = bg("#FFFFFF", 18f, "#E7EAF2"); elevation = dp(2).toFloat()
                setOnClickListener { startActivity(Intent(this@ToolsActivity, cls)) }
            }
            row.addView(TextView(this).apply { text = icon; textSize = 24f; gravity = Gravity.CENTER; background = bg("#EEF2FF", 14f); layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)) })
            val textBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(13), 0, dp(8), 0) }
            textBox.addView(TextView(this).apply { text = title; textSize = 16f; setTextColor(Color.parseColor("#17213D")); setTypeface(typeface, Typeface.BOLD) })
            textBox.addView(TextView(this).apply { text = detail; textSize = 12f; setTextColor(Color.parseColor("#72798F")); setPadding(0, dp(3), 0, 0) })
            row.addView(textBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(this).apply { text = "›"; textSize = 28f; setTextColor(Color.parseColor("#3157D5")); gravity = Gravity.CENTER })
            root.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) })
        }

        section("Image Tools", "Fast tools for forms, photos and documents")
        card("▦", "Image Merge", "Combine multiple images into one", ImageMergeActivity::class.java)
        card("✦", "Photo & Signature Maker", "Prepare exam-form photo and signature", PhotoSignatureToolActivity::class.java)
        card("⇩", "Target KB Compressor", "Compress an image to your target size", TargetKbToolActivity::class.java)
        section("PDF Tools", "Preview first, save only when you choose")
        card("PDF", "PDF Toolkit", "Merge, split, reorder, compress and convert", PdfToolkitActivity::class.java)
        section("Student & Result Tools", "Academic utilities with verification support")
        card("A+", "Professional Marksheet Maker", "Generate a polished marksheet with QR verification", MarksheetMakerActivity::class.java)
        card("✓", "Verify Marksheet", "Check an EduNexa verification ID", VerifyMarksheetActivity::class.java)
        card("%", "Marks & Percentage Calculator", "Calculate percentage and result quickly", ResultCalculatorActivity::class.java)
        root.addView(TextView(this).apply { text = "🔒  Nothing is saved automatically. Preview your output, then tap Save."; textSize = 12f; setTextColor(Color.parseColor("#52607A")); setPadding(dp(14), dp(14), dp(14), dp(14)); background = bg("#EEF2FF", 16f) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.parseColor("#F6F7FC")); addView(root) })
    }
}
