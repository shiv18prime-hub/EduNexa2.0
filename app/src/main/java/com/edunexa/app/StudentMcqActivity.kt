package com.edunexa.app

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.McqRepository
import com.google.android.material.card.MaterialCardView

class StudentMcqActivity : AppCompatActivity() {
    private val repo = McqRepository()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(TextView(this).apply {
            text = "MCQ Tests"
            textSize = 27f
            setTypeface(typeface, 1)
        })
        val status = TextView(this).apply {
            text = "Loading tests…"
            setPadding(0, dp(8), 0, dp(14))
        }
        root.addView(status)
        repo.studentTests({ tests ->
            status.text = if (tests.isEmpty()) "No active tests from your school" else "${tests.size} active test(s)"
            tests.forEach { (testId, testData) -> addTest(root, testId, testData) }
        }, { status.text = it })
    }

    private fun addTest(root: LinearLayout, testId: String, testData: Map<String, Any>) {
        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            setContentPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(TextView(this).apply {
            text = "${testData["title"]}\n${testData["subject"]} • ${testData["chapter"]}"
            textSize = 18f
            setTypeface(typeface, 1)
        })
        val timerText = TextView(this).apply {
            textSize = 16f
            setPadding(0, dp(8), 0, dp(8))
        }
        box.addView(timerText)
        val questions = testData["questions"] as? List<*> ?: emptyList<Any>()
        val groups = mutableListOf<RadioGroup>()
        questions.forEachIndexed { questionIndex, item ->
            val question = item as? Map<*, *> ?: return@forEachIndexed
            box.addView(TextView(this).apply {
                text = "${questionIndex + 1}. ${question["question"]}"
                textSize = 16f
                setPadding(0, dp(10), 0, dp(4))
            })
            val group = RadioGroup(this)
            (question["options"] as? List<*>)?.forEachIndexed { optionIndex, option ->
                group.addView(RadioButton(this).apply {
                    text = option.toString()
                    this.id = (questionIndex + 1) * 100 + optionIndex
                })
            }
            groups.add(group)
            box.addView(group)
        }
        val submit = Button(this).apply {
            text = "Submit Test"
            isAllCaps = false
        }
        box.addView(submit)
        val review = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(review)
        var submitted = false

        fun finishTest(auto: Boolean) {
            if (submitted) return
            submitted = true
            var score = 0
            val answers = mutableMapOf<String, Int>()
            questions.forEachIndexed { questionIndex, item ->
                val question = item as? Map<*, *> ?: return@forEachIndexed
                val selected = groups.getOrNull(questionIndex)?.checkedRadioButtonId?.let { checkedId ->
                    if (checkedId == -1) -1 else checkedId - (questionIndex + 1) * 100
                } ?: -1
                answers[questionIndex.toString()] = selected
                val correct = (question["correctIndex"] as? Number)?.toInt() ?: -2
                if (selected == correct) score++
                val options = question["options"] as? List<*>
                val selectedText = if (selected >= 0) options?.getOrNull(selected)?.toString() ?: "-" else "Not answered"
                val correctText = if (correct >= 0) options?.getOrNull(correct)?.toString() ?: "-" else "-"
                val explanation = question["explanation"]?.toString()?.trim().orEmpty()
                review.addView(TextView(this).apply {
                    text = "\nQ${questionIndex + 1}: ${if (selected == correct) "✓ Correct" else "✗ Wrong"}\nYour answer: $selectedText\nCorrect answer: $correctText${if (explanation.isNotBlank()) "\nExplanation: $explanation" else ""}"
                    textSize = 15f
                })
            }
            groups.forEach { group ->
                for (i in 0 until group.childCount) group.getChildAt(i).isEnabled = false
            }
            submit.isEnabled = false
            val percentage = if (questions.isNotEmpty()) score * 100.0 / questions.size else 0.0
            review.addView(TextView(this).apply {
                text = "\nResult: $score/${questions.size} • %.1f%%".format(percentage)
                textSize = 20f
                setTypeface(typeface, 1)
            })
            repo.submit(testId, answers, score, questions.size) { ok, message ->
                Toast.makeText(this, if (ok) "${if (auto) "Time up • " else ""}$message" else message, Toast.LENGTH_LONG).show()
            }
        }

        submit.setOnClickListener { finishTest(false) }
        val minutes = (testData["durationMinutes"] as? Number)?.toLong()?.coerceAtLeast(1) ?: 10L
        object : CountDownTimer(minutes * 60000, 1000) {
            override fun onTick(ms: Long) {
                val seconds = ms / 1000
                timerText.text = "Time left: %02d:%02d".format(seconds / 60, seconds % 60)
            }
            override fun onFinish() {
                timerText.text = "Time up"
                finishTest(true)
            }
        }.start()
        card.addView(box)
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, 0, 0, dp(12))
        root.addView(card, lp)
    }
}
