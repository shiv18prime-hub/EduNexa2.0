package com.edunexa.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.McqRepository

class PublishMcqActivity : AppCompatActivity() {
    private val repo=McqRepository()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_publish_mcq)
        val title=findViewById<EditText>(R.id.testTitleInput); val subject=findViewById<EditText>(R.id.testSubjectInput); val chapter=findViewById<EditText>(R.id.testChapterInput); val duration=findViewById<EditText>(R.id.testDurationInput)
        val question=findViewById<EditText>(R.id.questionInput); val a=findViewById<EditText>(R.id.optionAInput); val b=findViewById<EditText>(R.id.optionBInput); val c=findViewById<EditText>(R.id.optionCInput); val d=findViewById<EditText>(R.id.optionDInput)
        val correct=findViewById<Spinner>(R.id.correctOptionSpinner); correct.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("Correct: A","Correct: B","Correct: C","Correct: D"))
        val button=findViewById<Button>(R.id.publishTestBtn); val progress=findViewById<ProgressBar>(R.id.testProgress); val status=findViewById<TextView>(R.id.testStatus)
        button.setOnClickListener {
            if(title.text.isBlank()||subject.text.isBlank()||question.text.isBlank()||a.text.isBlank()||b.text.isBlank()||c.text.isBlank()||d.text.isBlank()) { status.text="Complete all required fields"; return@setOnClickListener }
            val q=mapOf<String,Any>("question" to question.text.toString().trim(),"options" to listOf(a.text.toString(),b.text.toString(),c.text.toString(),d.text.toString()),"correctIndex" to correct.selectedItemPosition)
            button.isEnabled=false; progress.visibility=View.VISIBLE; status.text="Publishing test…"
            repo.publishTest(title.text.toString(),subject.text.toString(),chapter.text.toString(),duration.text.toString().toIntOrNull()?:10,listOf(q)) { ok,msg -> progress.visibility=View.GONE;button.isEnabled=true;status.text=msg }
        }
    }
}
