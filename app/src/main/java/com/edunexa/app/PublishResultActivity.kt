package com.edunexa.app

import android.text.InputType
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.SchoolResultRepository

class PublishResultActivity : AppCompatActivity() {
    private val repo = SchoolResultRepository()
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var student: Spinner
    private lateinit var exam: EditText
    private lateinit var subject: EditText
    private lateinit var obtained: EditText
    private lateinit var total: EditText
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(28))}
        setContentView(ScrollView(this).apply{addView(root)})
        root.addView(TextView(this).apply{text="Result Management";textSize=27f;setTypeface(typeface,1)})
        root.addView(TextView(this).apply{text="Publish individual subject results and manage published records.";setPadding(0,dp(5),0,dp(12))})
        student=Spinner(this); exam=EditText(this).apply{hint="Exam / Term"}; subject=EditText(this).apply{hint="Subject"}
        obtained=EditText(this).apply{hint="Obtained marks";inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL}
        total=EditText(this).apply{hint="Total marks";inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL}
        listOf(student,exam,subject,obtained,total).forEach{root.addView(it)}
        root.addView(Button(this).apply{text="Publish Result";isAllCaps=false;setOnClickListener{publish()}})
        root.addView(Button(this).apply{text="Refresh Published Results";isAllCaps=false;setOnClickListener{loadPublished()}})
        status=TextView(this).apply{setPadding(0,dp(10),0,dp(10))};root.addView(status)
        loadStudents(); loadPublished()
    }

    private fun loadStudents(){repo.students({rows->student.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,rows.map{StudentItem(it.first,it.second)});if(rows.isEmpty())status.text="No connected students"},{status.text=it})}
    private fun publish(){val item=student.selectedItem as? StudentItem?:return;val a=obtained.text.toString().toDoubleOrNull()?:-1.0;val b=total.text.toString().toDoubleOrNull()?:0.0;if(exam.text.isBlank()||subject.text.isBlank()||a<0||b<=0||a>b){status.text="Enter valid exam, subject and marks";return};repo.publish(item.id,item.name,exam.text.toString(),subject.text.toString(),a,b){ok,m->status.text=m;if(ok){subject.text.clear();obtained.text.clear();total.text.clear();loadPublished()}}}
    private fun loadPublished(){repo.schoolResults({rows->
        while(root.childCount>10)root.removeViewAt(10)
        root.addView(TextView(this).apply{text="Published Results (${rows.size})";textSize=20f;setTypeface(typeface,1)})
        rows.forEach{r->val id=r["id"].toString();val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),dp(10),dp(10),dp(10))};box.addView(TextView(this).apply{text="${r["studentName"]} • ${r["exam"]}\n${r["subject"]}: ${r["obtained"]}/${r["total"]} • ${"%.1f".format((r["percentage"] as? Number)?.toDouble()?:0.0)}%"});box.addView(Button(this).apply{text="Unpublish / Remove";isAllCaps=false;setOnClickListener{repo.deleteResult(id){_,m->Toast.makeText(this@PublishResultActivity,m,Toast.LENGTH_SHORT).show();loadPublished()}}});root.addView(box)}
    },{status.text=it})}
    data class StudentItem(val id:String,val name:String){override fun toString()=name}
}
