package com.edunexa.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.activity.OnBackPressedCallback
import com.edunexa.app.core.PracticeQuiz
import com.edunexa.app.core.PracticeSet
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import java.util.UUID

class PracticeActivity : EduActivity() {
    private val ui by lazy { StudyUi(this) }
    private val prefs by lazy { getSharedPreferences("practice-${FirebaseAuth.getInstance().currentUser?.uid ?: "guest"}", MODE_PRIVATE) }
    private var topic: PracticeSet? = null
    private var answers = intArrayOf()
    private var index = 0
    private var submitted = false
    private var attemptId = ""

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { leave() } })
        topic = PracticeQuiz.sets.find { it.id == state?.getString("topic") }
        topic?.let { set ->
            answers = state?.getIntArray("answers")?.takeIf { values -> values.size == set.questions.size && values.indices.all { values[it] == -1 || values[it] in set.questions[it].options.indices } } ?: IntArray(set.questions.size) { -1 }
            index = (state?.getInt("index") ?: 0).coerceIn(set.questions.indices)
            submitted = state?.getBoolean("submitted") ?: false
            attemptId = state?.getString("attempt") ?: UUID.randomUUID().toString()
        }
        when { topic == null -> library(); submitted -> results(); else -> question() }
    }

    private fun records(): List<JSONObject> = runCatching {
        val array = JSONArray(prefs.getString("history", "[]"))
        (0 until array.length()).map { array.getJSONObject(it) }
    }.getOrDefault(emptyList())

    private fun library() {
        topic = null
        val root = ui.screen("Practice studio", "Build confidence with short quizzes and clear explanations.") { finish() }
        root.addView(ui.text("OFFLINE • NO SIGN-IN REQUIRED", 11f, true, "#157347"))
        PracticeQuiz.sets.forEach { set ->
            ui.action(root, "${set.questions.size} QUESTIONS", set.title, set.description) {
                topic = set; answers = IntArray(set.questions.size) { -1 }; index = 0; submitted = false
                attemptId = UUID.randomUUID().toString(); question()
            }
        }
        root.addView(ui.text("Recent practice", 21f, true))
        val history = records()
        if (history.isEmpty()) root.addView(ui.text("Your completed quizzes will appear here. Scores are personal practice, not school exam records.", 14f, color = "#667085"))
        history.forEach { item ->
            val box = ui.panel(root)
            box.addView(ui.text(item.optString("title"), 17f, true))
            box.addView(ui.text("${item.optInt("correct")} / ${item.optInt("total")} correct", 20f, true, "#4338CA"))
            box.addView(ui.text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.optLong("at"))), 12f, color = "#667085"))
        }
    }

    private fun question() {
        val set = topic ?: return library()
        val q = set.questions[index]
        val root = ui.screen(set.title, "Personal practice • ${index + 1} of ${set.questions.size}") { leave() }
        root.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = set.questions.size; progress = index + 1 }, LinearLayout.LayoutParams(-1, ui.dp(6)).apply { bottomMargin = ui.dp(18) })
        val box = ui.panel(root)
        box.addView(ui.text("QUESTION ${index + 1}", 11f, true, "#6258C7"))
        box.addView(ui.text(q.prompt, 22f, true))
        val group = RadioGroup(this)
        q.options.forEachIndexed { choice, label ->
            group.addView(RadioButton(this).apply {
                id = View.generateViewId(); text = label; textSize = 16f; tag = choice
                minHeight = ui.dp(56); setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8))
                isChecked = answers[index] == choice
            }, RadioGroup.LayoutParams(-1, -2))
        }
        group.setOnCheckedChangeListener { _, id -> if (id != -1) answers[index] = group.findViewById<RadioButton>(id).tag as Int }
        box.addView(group)
        if (index > 0) ui.button(root, "Previous question") { index--; question() }
        ui.button(root, if (index == set.questions.lastIndex) "Finish practice" else "Next question") {
            if (index < set.questions.lastIndex) { index++; question() } else {
                val skipped = answers.count { it == -1 }
                if (skipped > 0) AlertDialog.Builder(this).setTitle("Finish with $skipped unanswered?")
                    .setMessage("Unanswered questions count as skipped. You can review every answer afterwards.")
                    .setNegativeButton("Keep practising", null).setPositiveButton("Finish") { _, _ -> submit() }.show()
                else submit()
            }
        }
        root.addView(ui.text("${answers.count { it >= 0 }} answered • You can go back before submitting.", 12f, color = "#667085"))
    }

    private fun submit() {
        if (submitted) return
        val set = topic ?: return
        val score = PracticeQuiz.score(set.questions, answers.toList())
        val item = JSONObject().put("id", attemptId).put("title", set.title).put("correct", score.correct).put("total", score.total).put("at", System.currentTimeMillis())
        prefs.edit().putString("history", JSONArray((listOf(item) + records().filter { it.optString("id") != attemptId }).take(20)).toString()).apply()
        submitted = true; results()
    }

    private fun results() {
        val set = topic ?: return library()
        val score = PracticeQuiz.score(set.questions, answers.toList())
        val root = ui.screen("Practice complete", set.title) { library() }
        val summary = ui.panel(root)
        summary.addView(ui.text("YOUR SCORE", 11f, true, "#6258C7"))
        summary.addView(ui.text("${score.correct} / ${score.total}", 42f, true, "#4338CA"))
        summary.addView(ui.text("%.0f%% • ${score.wrong} incorrect • ${score.skipped} skipped".format(score.percentage)))
        summary.addView(ui.text("Saved on this device. This is a practice score, not an official school result.", 12f, color = "#667085"))
        ui.button(root, "Share practice score") {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "EduNexa practice • ${set.title}\n${score.correct}/${score.total} correct (%.0f%%)\nPersonal practice score".format(score.percentage)) }, "Share score"))
        }
        ui.button(root, "Choose another quiz") { library() }
        root.addView(ui.text("Review & learn", 21f, true))
        set.questions.forEachIndexed { i, q ->
            val box = ui.panel(root)
            val right = answers[i] == q.correct
            box.addView(ui.text("${i + 1}. ${q.prompt}", 17f, true))
            box.addView(ui.text("Your answer: ${q.options.getOrNull(answers[i]) ?: "Skipped"}", 14f, color = if (right) "#157347" else "#B42318"))
            box.addView(ui.text("Correct: ${q.options[q.correct]}", 14f, true, "#157347"))
            box.addView(ui.text(q.explanation, 14f, color = "#667085"))
        }
    }

    private fun leave() {
        if (topic == null) finish() else if (submitted) library() else AlertDialog.Builder(this)
            .setTitle("Leave this practice?").setMessage("Your unfinished answers will be discarded.")
            .setNegativeButton("Stay", null).setPositiveButton("Leave") { _, _ -> library() }.show()
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString("topic", topic?.id); out.putIntArray("answers", answers); out.putInt("index", index)
        out.putBoolean("submitted", submitted); out.putString("attempt", attemptId); super.onSaveInstanceState(out)
    }
}
