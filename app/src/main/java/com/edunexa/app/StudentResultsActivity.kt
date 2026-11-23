package com.edunexa.app

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import com.edunexa.app.data.SchoolResultRepository
import com.google.android.material.card.MaterialCardView

class StudentResultsActivity : EduActivity() {
    private val repo = SchoolResultRepository()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        setContentView(ScrollView(this).apply { addView(root) })

        root.addView(TextView(this).apply {
            text = "My Performance"
            textSize = 27f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#20243A"))
        })
        val status = TextView(this).apply {
            text = "Published school results"
            setPadding(0, dp(8), 0, dp(10))
        }
        root.addView(status)
        val refresh = Button(this).apply {
            text = "Refresh Results"
            isAllCaps = false
        }
        root.addView(refresh)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content)

        fun load() {
            status.text = "Loading published results…"
            content.removeAllViews()
            repo.myResults({ rows ->
                content.removeAllViews()
                if (rows.isEmpty()) {
                    status.text = "No published results yet"
                    content.addView(TextView(this).apply {
                        text = "Your school-published marks will appear here when available."
                        textSize = 15f
                        setPadding(0, dp(14), 0, dp(14))
                    })
                    return@myResults
                }
                val percentages = rows.map { (it["percentage"] as? Number)?.toDouble() ?: 0.0 }
                val average = percentages.average()
                val failed = rows.count { it["result"] == "FAIL" }
                status.text = "${rows.size} published result(s) • Subject-wise comparison"
                val summary = MaterialCardView(this).apply {
                    radius = dp(20).toFloat()
                    setCardBackgroundColor(Color.parseColor("#F4F3FF"))
                    setContentPadding(dp(17), dp(16), dp(17), dp(16))
                    addView(TextView(this@StudentResultsActivity).apply {
                        text = "Overall Average  ${"%.1f".format(average)}%\nSubjects needing improvement: $failed\n${if (failed == 0) "All published results are in passing range" else "Focus on the subjects marked below"}"
                        textSize = 16f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(Color.parseColor("#4338CA"))
                    })
                }
                content.addView(summary, LinearLayout.LayoutParams(-1, -2).apply {
                    setMargins(0, 0, 0, dp(14))
                })
                rows.forEachIndexed { i, result ->
                    val percentage = (result["percentage"] as? Number)?.toDouble() ?: 0.0
                    val subject = result["subject"].toString()
                    val previous = rows.drop(i + 1).firstOrNull {
                        it["subject"].toString().equals(subject, true)
                    }
                    val prevPercent = previous?.let { (it["percentage"] as? Number)?.toDouble() }
                    val trend = prevPercent?.let { percentage - it }
                    val trendText = trend?.let {
                        "\n${if (it >= 0) "↑ Improved" else "↓ Dropped"} ${"%.1f".format(kotlin.math.abs(it))}% vs previous $subject result"
                    } ?: "\nFirst recorded $subject result"
                    val card = MaterialCardView(this).apply {
                        radius = dp(18).toFloat()
                        cardElevation = dp(2).toFloat()
                        setCardBackgroundColor(Color.WHITE)
                        setContentPadding(dp(16), dp(16), dp(16), dp(16))
                        addView(TextView(this@StudentResultsActivity).apply {
                            text = "${result["exam"]} • $subject\n${result["obtained"]} / ${result["total"]}\n${"%.2f".format(percentage)}% • Grade ${result["grade"]} • ${if (result["result"] == "PASS") "PASS" else "NEEDS IMPROVEMENT"}$trendText"
                            textSize = 16f
                        })
                    }
                    content.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                        setMargins(0, 0, 0, dp(11))
                    })
                }
            }, { error ->
                status.text = error
                content.removeAllViews()
                content.addView(TextView(this).apply {
                    text = "Unable to load results. Please check your connection and retry."
                    textSize = 15f
                    setPadding(0, dp(12), 0, dp(12))
                })
            })
        }
        refresh.setOnClickListener { load() }
        load()
    }
}
