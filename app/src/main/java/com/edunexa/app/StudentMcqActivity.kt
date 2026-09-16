package com.edunexa.app

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.McqRepository
import com.google.android.material.card.MaterialCardView

class StudentMcqActivity:AppCompatActivity(){
 private val repo=McqRepository(); private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;padding=dp(18)};setContentView(ScrollView(this).apply{addView(root)})
  root.addView(TextView(this).apply{text="MCQ Tests";textSize=27f;setTypeface(typeface,1)})
  val status=TextView(this).apply{text="Loading tests…";setPadding(0,dp(8),0,dp(14))};root.addView(status)
  repo.studentTests({tests->status.text=if(tests.isEmpty())"No active tests from your school" else "${tests.size} active test(s)";tests.forEach{(id,t)->
   val card=MaterialCardView(this).apply{radius=dp(18).toFloat();cardElevation=dp(3).toFloat();setCardBackgroundColor(Color.WHITE);setContentPadding(dp(16),dp(16),dp(16),dp(16))};val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
   box.addView(TextView(this).apply{text="${t["title"]}\n${t["subject"]} • ${t["chapter"]}\n${t["durationMinutes"]} min";textSize=17f;setTypeface(typeface,1)})
   val questions=t["questions"] as? List<*> ?: emptyList<Any>(); if(questions.isNotEmpty()){val q=questions[0] as? Map<*,*>;val opts=q?.get("options") as? List<*>;box.addView(TextView(this).apply{text="\n${q?.get("question")}";textSize=16f});val group=RadioGroup(this);opts?.forEachIndexed{i,o->group.addView(RadioButton(this).apply{text=o.toString();id=1000+i})};box.addView(group);box.addView(Button(this).apply{text="Submit Test";isAllCaps=false;setOnClickListener{val selected=group.checkedRadioButtonId-1000;if(selected !in 0..3){Toast.makeText(this@StudentMcqActivity,"Select an answer",Toast.LENGTH_SHORT).show();return@setOnClickListener};val correct=(q?.get("correctIndex") as? Number)?.toInt()?:-1;val score=if(selected==correct)1 else 0;repo.submit(id,mapOf("0" to selected),score,1){ok,msg->Toast.makeText(this@StudentMcqActivity,if(ok)"$msg • Score $score/1" else msg,Toast.LENGTH_LONG).show()}}})}
   card.addView(box);val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(12));root.addView(card,lp)
  }},{status.text=it})
 }
}
