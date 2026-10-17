package com.edunexa.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/** Small shared native components for the study and teacher screens. */
class StudyUi(private val activity: Activity) {
    fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
    fun text(value: String, size: Float = 15f, bold: Boolean = false, color: String = "#17213D") = TextView(activity).apply {
        text = value; textSize = size; setTextColor(Color.parseColor(color))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(4), 0, dp(6))
    }
    fun screen(title: String, subtitle: String, back: (() -> Unit)? = null): LinearLayout {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(12), dp(20), dp(28))
        }
        if (back != null) root.addView(text("‹  Back", 15f, true, "#4338CA").apply {
            minHeight = dp(48); gravity = Gravity.CENTER_VERTICAL; isFocusable = true; setOnClickListener { back() }
        })
        root.addView(text("EDUNEXA  /  LEARN & GROW", 10f, true, "#6258C7").apply { letterSpacing = .12f })
        root.addView(text(title, 28f, true))
        root.addView(text(subtitle, 14f, color = "#667085").apply { setPadding(0, dp(2), 0, dp(18)) })
        activity.setContentView(ScrollView(activity).apply { setBackgroundColor(Color.parseColor("#F6F7FC")); isFillViewport = true; addView(root) })
        return root
    }
    fun panel(parent: LinearLayout): LinearLayout {
        val card = MaterialCardView(activity).apply {
            radius = dp(20).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
            strokeColor = Color.parseColor("#E3E7F0"); setCardBackgroundColor(Color.WHITE)
        }
        val box = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(16)) }
        card.addView(box); parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        return box
    }
    fun button(parent: LinearLayout, label: String, action: () -> Unit): MaterialButton {
        val button = MaterialButton(activity).apply {
            text = label; isAllCaps = false; textSize = 15f; cornerRadius = dp(14); minHeight = dp(52)
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        return button
    }
    fun action(parent: LinearLayout, badge: String, title: String, detail: String, open: () -> Unit) {
        val box = panel(parent)
        box.addView(text(badge, 11f, true, "#6258C7")); box.addView(text(title, 19f, true))
        box.addView(text(detail, 13f, color = "#667085")); button(box, "Open  ›", open)
    }
}
