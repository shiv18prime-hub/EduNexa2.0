package com.edunexa.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.NoticeRepository

class PublishNoticeActivity : AppCompatActivity() {
    private val repo=NoticeRepository()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_publish_notice)
        val title=findViewById<EditText>(R.id.noticeTitleInput); val message=findViewById<EditText>(R.id.noticeMessageInput)
        val target=findViewById<EditText>(R.id.noticeClassInput); val important=findViewById<CheckBox>(R.id.importantCheck)
        val button=findViewById<Button>(R.id.publishNoticeBtn); val progress=findViewById<ProgressBar>(R.id.publishNoticeProgress); val status=findViewById<TextView>(R.id.publishNoticeStatus)
        button.setOnClickListener {
            if(title.text.isBlank()||message.text.isBlank()) { status.text="Title and message are required"; return@setOnClickListener }
            button.isEnabled=false; progress.visibility=View.VISIBLE; status.text="Publishing…"
            repo.publish(title.text.toString(),message.text.toString(),target.text.toString(),important.isChecked) { ok,text ->
                progress.visibility=View.GONE; button.isEnabled=true; status.text=text
                if(ok) { title.text.clear(); message.text.clear(); target.text.clear(); important.isChecked=false }
            }
        }
    }
}
