package com.edunexa.app

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.McqRepository
import com.google.android.material.card.MaterialCardView

class StudentMcqActivity:AppCompatActivity(){
 private val repo=McqRepository(); private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;padding=dp(18)};setContentView(ScrollView(this).apply{addView(root)});root.addView(TextView(this).apply{text="MCQ Tests";textSize=27f;setTypeface(typeface,1)});val status=TextView(this).apply{text="Loading tests…";setPadding(0,dp(8),0,dp(14))};root.addView(status)
  repo.studentTests({tests->status.text=if(tests.isEmpty())"No active tests from your school" else "${tests.size} active test(s)";tests.forEach{(id,t)->addTest(root,id,t)}},{status.text=it})
 }
 private fun addTest(root:LinearLayout,id:String,t:Map<String,Any>){val card=MaterialCardView(this).apply{radius=dp(18).toFloat();cardElevation=dp(3).toFloat();setCardBackgroundColor(Color.WHITE);setContentPadding(dp(16),dp(16),dp(16),dp(16))};val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};box.addView(TextView(this).apply{text="${t["title"]}\n${t["subject"]} • ${t["chapter"]}";textSize=18f;setTypeface(typeface,1)});val timerText=TextView(this).apply{textSize=16f;setPadding(0,dp(8),0,dp(8))};box.addView(timerText)
  val questions=t["questions"] as? List<*>?:emptyList<Any>();val groups=mutableListOf<RadioGroup>();questions.forEachIndexed{qi,item->val q=item as? Map<*,*>?:return@forEachIndexed;box.addView(TextView(this).apply{text="${qi+1}. ${q["question"]}";textSize=16f;setPadding(0,dp(10),0,dp(4))});val group=RadioGroup(this);(q["options"] as? List<*>)?.forEachIndexed{oi,o->group.addView(RadioButton(this).apply{text=o.toString();id=(qi+1)*100+oi})};groups.add(group);box.addView(group)}
  val submit=Button(this).apply{text="Submit Test";isAllCaps=false};box.addView(submit);var submitted=false
  fun finishTest(auto:Boolean){if(submitted)return;submitted=true;var score=0;val answers=mutableMapOf<String,Int>();questions.forEachIndexed{qi,item->val selected=groups.getOrNull(qi)?.checkedRadioButtonId?.let{if(it==-1)-1 else it-(qi+1)*100}?:-1;answers[qi.toString()]=selected;val correct=((item as? Map<*,*>)?.get("correctIndex") as? Number)?.toInt()?:-2;if(selected==correct)score++};submit.isEnabled=false;repo.submit(id,answers,score,questions.size){ok,msg->Toast.makeText(this,if(ok)"${if(auto)"Time up • " else ""}$msg • Score $score/${questions.size}" else msg,Toast.LENGTH_LONG).show()}}
  submit.setOnClickListener{finishTest(false)};val minutes=(t["durationMinutes"] as? Number)?.toLong()?.coerceAtLeast(1)?:10L;object:CountDownTimer(minutes*60000,1000){override fun onTick(ms:Long){val s=ms/1000;timerText.text="Time left: %02d:%02d".format(s/60,s%60)};override fun onFinish(){timerText.text="Time up";finishTest(true)}}.start();card.addView(box);val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(12));root.addView(card,lp)}
}
