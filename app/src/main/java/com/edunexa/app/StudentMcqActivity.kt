package com.edunexa.app
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import com.edunexa.app.data.Backend
import com.edunexa.app.data.McqRepository
import org.json.JSONObject

class StudentMcqActivity:EduActivity(){
    private val repo=McqRepository();private lateinit var root:LinearLayout;private lateinit var status:TextView;private var timer:CountDownTimer?=null;private var sessionId:String?=null;private var deadline=0L;private var testTitle="";private var questions:List<Map<String,Any>> = emptyList();private val answers=mutableMapOf<String,Int>();private var submitting=false
    override fun onCreate(state:Bundle?){super.onCreate(state);root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,24,28,28)};setContentView(ScrollView(this).apply{addView(root)});root.addView(TextView(this).apply{text="School Tests";textSize=27f;setTypeface(typeface,android.graphics.Typeface.BOLD)});status=TextView(this);root.addView(status)
        sessionId=state?.getString("session");deadline=state?.getLong("deadline")?:0;testTitle=state?.getString("title")?:"";state?.getString("answers")?.let{runCatching{val o=JSONObject(it);o.keys().forEach{k->answers[k]=o.getInt(k)}}};if(sessionId!=null){questions=Backend.rows(state?.getString("questions")?.let{org.json.JSONArray(it).let{a->(0 until a.length()).map{i->val o=a.getJSONObject(i);o.keys().asSequence().associateWith{k->o.get(k).let{v->if(v is org.json.JSONArray)(0 until v.length()).map{j->v.getString(j)} else v}}}}});showSession()}else loadTests()
    }
    private fun loadTests(){status.text="Loading tests…";repo.studentTests({tests->status.text=if(tests.isEmpty())"No active tests from your school" else "${tests.size} test(s) available";tests.forEach{(id,t)->root.addView(Button(this).apply{text="${t["title"]}\n${t["questionCount"]} questions • ${t["durationMinutes"]} minutes";isAllCaps=false;setOnClickListener{isEnabled=false;repo.start(id,{r->sessionId=r["sessionId"].toString();deadline=(r["deadline"] as Number).toLong();testTitle=r["title"].toString();questions=Backend.rows(r["questions"]);answers.clear();showSession()},{error->isEnabled=true;status.text=error})}})}},{status.text=it})}
    private fun showSession(){timer?.cancel();while(root.childCount>2)root.removeViewAt(2);status.text=testTitle;val time=TextView(this).apply{textSize=18f};root.addView(time)
        val controls=mutableListOf<RadioGroup>();questions.forEachIndexed{i,q->root.addView(TextView(this).apply{text="${i+1}. ${q["question"]}";textSize=17f;setPadding(0,18,0,4)});val group=RadioGroup(this);(q["options"] as? List<*>)?.forEachIndexed{j,o->group.addView(RadioButton(this).apply{id=android.view.View.generateViewId();text=o.toString();tag=j;isChecked=answers[i.toString()]==j})};group.setOnCheckedChangeListener{g,id->if(id!=-1)answers[i.toString()]=g.findViewById<RadioButton>(id).tag as Int};controls.add(group);root.addView(group)}
        val submit=Button(this).apply{text="Submit Test"};root.addView(submit);val output=TextView(this).apply{textSize=20f};root.addView(output)
        fun send(){if(submitting)return;val sid=sessionId?:return;submitting=true;submit.isEnabled=false;repo.submit(sid,answers.toMap(),{r->timer?.cancel();controls.forEach{g->for(i in 0 until g.childCount)g.getChildAt(i).isEnabled=false};val score=r["result"] as? Map<*,*>;output.text="Score: ${score?.get("score")}/${score?.get("total")} • %.1f%%".format((score?.get("percentage") as? Number)?.toDouble()?:0.0);status.text="Submitted";sessionId=null;submitting=false},{error->submitting=false;submit.isEnabled=true;status.text=error})}
        submit.setOnClickListener{send()};val remaining=(deadline-System.currentTimeMillis()).coerceAtLeast(0);timer=object:CountDownTimer(remaining,1000){override fun onTick(ms:Long){time.text="Time left: %02d:%02d".format(ms/60000,ms/1000%60)};override fun onFinish(){time.text="Time up";send()}}.start()
    }
    override fun onSaveInstanceState(out:Bundle){out.putString("session",sessionId);out.putLong("deadline",deadline);out.putString("title",testTitle);out.putString("questions",org.json.JSONArray(questions.map{JSONObject(it)}).toString());out.putString("answers",JSONObject(answers as Map<*,*>).toString());super.onSaveInstanceState(out)}
    override fun onDestroy(){timer?.cancel();super.onDestroy()}
}
