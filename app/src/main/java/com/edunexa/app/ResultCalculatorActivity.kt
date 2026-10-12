package com.edunexa.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.edunexa.app.core.MarksCalculator
import com.edunexa.app.core.SubjectMarks
import com.edunexa.app.data.ResultRepository
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

class ResultCalculatorActivity : EduActivity() {
    private val repo = ResultRepository()
    private lateinit var subjectBox: LinearLayout
    private lateinit var history: LinearLayout
    private lateinit var exam: EditText
    private lateinit var output: TextView
    private lateinit var saveButton: Button
    private var shareText = ""
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(24)) }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(TextView(this).apply { text="Result Studio"; textSize=27f; setTypeface(typeface,android.graphics.Typeface.BOLD) })
        root.addView(TextView(this).apply { text="Add subjects, set passing marks and share your result."; setPadding(0,dp(8),0,dp(12)) })
        exam=EditText(this).apply { hint="Exam / term name" }; root.addView(exam)
        subjectBox=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; root.addView(subjectBox)
        root.addView(Button(this).apply { text="＋ Add Subject"; setOnClickListener { if(subjectBox.childCount<30)addSubject() } })
        output=TextView(this).apply { textSize=20f; setPadding(dp(12),dp(16),dp(12),dp(16)); setBackgroundColor(Color.parseColor("#EEF0FF")) }; root.addView(output)
        fun calculate(): List<SubjectMarks>? = try {
            val rows=rows(); val r=MarksCalculator.calculate(rows)
            output.text="${r.obtained} / ${r.total}  •  %.2f%%\nGrade ${r.grade}  •  ${if(r.passed) "PASS" else "FAIL"}".format(r.percentage)
            shareText="EduNexa Result\n${FirebaseAuth.getInstance().currentUser?.displayName ?: "Student"}\n${exam.text.toString().ifBlank { "My result" }}\n"+rows.joinToString("\n") { "${it.name}: ${it.obtained}/${it.max} (Pass: ${it.passMarks})${if(it.optional) " • Optional" else ""}" }+"\n\n${output.text}\nCreated with EduNexa 2.0"
            rows
        } catch(e:Exception){ output.text=e.message ?: "Enter valid marks"; null }
        root.addView(Button(this).apply { text="Calculate"; setOnClickListener { calculate() } })
        saveButton=Button(this).apply { text="Save & Sync Result"; setOnClickListener { val rows=calculate() ?: return@setOnClickListener; isEnabled=false; repo.saveSubjects(exam.text.toString().ifBlank { "My result" },rows) { ok,msg -> isEnabled=true; Toast.makeText(this@ResultCalculatorActivity,msg,Toast.LENGTH_LONG).show(); if(ok)loadHistory() } } }; root.addView(saveButton)
        root.addView(Button(this).apply { text="Share Result Card"; setOnClickListener { if(calculate()!=null)startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,shareText) },"Share result")) } })
        root.addView(TextView(this).apply { text="Saved Results"; textSize=21f; setPadding(0,dp(16),0,dp(8)) })
        root.addView(Button(this).apply { text="Refresh History"; setOnClickListener { loadHistory() } })
        history=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; root.addView(history)
        val draft=state?.getString("subjects")
        if(draft!=null)runCatching { val a=JSONArray(draft); for(i in 0 until a.length()){val x=a.getJSONObject(i);addSubject(x.optString("name"),x.optInt("max",100),x.optInt("obtained"),x.optInt("passMarks",33),x.optBoolean("optional"))} }.onFailure { addSubject() } else repeat(3){addSubject()}
        exam.setText(state?.getString("exam") ?: "")
        loadHistory()
    }
    private fun addSubject(name:String="",max:Int=100,got:Int=0,pass:Int=33,optional:Boolean=false){
        val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(8),dp(10),dp(8),dp(8)) }
        val n=EditText(this).apply { hint="Subject name"; setText(name) };card.addView(n)
        val row=LinearLayout(this); fun number(h:String,v:Int)=EditText(this).apply { hint=h; inputType=InputType.TYPE_CLASS_NUMBER; setText(v.toString()); row.addView(this,LinearLayout.LayoutParams(0,-2,1f)) }
        val m=number("Max",max);val g=number("Obtained",got);val p=number("Passing",pass);card.addView(row)
        val o=CheckBox(this).apply { text="Optional (excluded from total)"; isChecked=optional };card.addView(o)
        card.addView(Button(this).apply { text="Remove Subject"; setOnClickListener { subjectBox.removeView(card) } });card.tag=listOf(n,m,g,p,o);subjectBox.addView(card)
    }
    private fun rows(): List<SubjectMarks> {
        return (0 until subjectBox.childCount).map { i ->
            val fields = subjectBox.getChildAt(i).tag as List<*>
            fun number(j: Int): Int {
                return (fields[j] as EditText).text.toString().toIntOrNull()
                    ?: throw IllegalArgumentException("Fill marks in every row")
            }
            SubjectMarks((fields[0] as EditText).text.toString().trim(), number(1), number(2), number(3), (fields[4] as CheckBox).isChecked)
        }
    }
    private fun loadHistory(){history.removeAllViews();history.addView(TextView(this).apply { text="Loading…" });repo.history({rows->history.removeAllViews();if(rows.isEmpty())history.addView(TextView(this).apply { text="No saved results yet" });rows.forEach { r-> val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(0,dp(10),0,dp(10)) };box.addView(TextView(this).apply { text="${r["exam"] ?: "Result"}\n${r["obtained"]}/${r["total"]} • %.2f%% • ${r["grade"]} • ${r["result"] ?: ""}".format((r["percentage"] as? Number)?.toDouble() ?: 0.0) });box.addView(Button(this).apply { text="Remove";setOnClickListener { AlertDialog.Builder(this@ResultCalculatorActivity).setMessage("Remove this saved result?").setNegativeButton("Cancel",null).setPositiveButton("Remove") { _,_->repo.delete(r["id"].toString()){ok,msg->Toast.makeText(this@ResultCalculatorActivity,msg,Toast.LENGTH_SHORT).show();if(ok)loadHistory()} }.show() } });history.addView(box) } },{error->history.removeAllViews();history.addView(TextView(this).apply {text=error})})}
    override fun onSaveInstanceState(out:Bundle){out.putString("exam",exam.text.toString());runCatching { out.putString("subjects",JSONArray(rows().map { JSONObject(it.asMap()) }).toString()) };super.onSaveInstanceState(out)}
}
