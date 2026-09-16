package com.edunexa.app

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.NoticeRepository
import com.google.android.material.card.MaterialCardView

class NoticeActivity : AppCompatActivity() {
    private val repo=NoticeRepository()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_notice)
        val list=findViewById<LinearLayout>(R.id.noticeList); val status=findViewById<TextView>(R.id.noticeStatus); val progress=findViewById<ProgressBar>(R.id.noticeProgress)
        repo.studentNotices({ notices ->
            progress.visibility=View.GONE; status.text=if(notices.isEmpty()) "No notices yet" else "${notices.size} notice(s)"
            notices.forEach { n ->
                val important=n["important"]==true
                val card=MaterialCardView(this).apply { radius=dp(18).toFloat(); cardElevation=dp(3).toFloat(); setCardBackgroundColor(Color.WHITE); setContentPadding(dp(18),dp(16),dp(18),dp(16)) }
                val text=TextView(this).apply { this.text=(if(important) "IMPORTANT • " else "")+"${n["title"]}\n\n${n["message"]}"+(n["targetClass"]?.toString()?.takeIf{it.isNotBlank()}?.let{"\n\nClass: $it"}?:""); textSize=16f; setTextColor(Color.rgb(42,46,68)) }
                card.addView(text); val lp=LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,dp(12)); list.addView(card,lp)
            }
        }, { error -> progress.visibility=View.GONE; status.text=error })
    }
}
