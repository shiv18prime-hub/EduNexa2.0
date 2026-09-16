package com.edunexa.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.ResultRepository
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    private lateinit var content: FrameLayout
    private val results = ResultRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main); content=findViewById(R.id.content)
        findViewById<Button>(R.id.homeBtn).setOnClickListener { showHome() }
        findViewById<Button>(R.id.toolsBtn).setOnClickListener { showTools() }
        findViewById<Button>(R.id.connectBtn).setOnClickListener { startActivity(Intent(this,JoinSchoolActivity::class.java)) }
        findViewById<Button>(R.id.profileBtn).setOnClickListener { showHistory() }
        showHome()
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun card()=MaterialCardView(this).apply { radius=dp(22).toFloat(); cardElevation=dp(3).toFloat(); setCardBackgroundColor(Color.WHITE); setContentPadding(dp(18),dp(18),dp(18),dp(18)) }

    private fun showHome() {
        content.removeAllViews(); val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        root.addView(TextView(this).apply { text="Hi, Student 👋"; textSize=25f; setTextColor(Color.rgb(25,31,55)); setTypeface(typeface,1) })
        root.addView(TextView(this).apply { text="Ready to calculate your result?"; setTextColor(Color.GRAY); setPadding(0,dp(4),0,dp(16)) })
        val c=card(); val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        box.addView(TextView(this).apply { text="Result Calculator"; textSize=21f; setTypeface(typeface,1) })
        val obtained=EditText(this).apply { hint="Obtained marks"; inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val total=EditText(this).apply { hint="Total marks"; inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val result=TextView(this).apply { textSize=23f; gravity=Gravity.CENTER; setPadding(0,dp(16),0,0); setTextColor(Color.rgb(67,56,202)); setTypeface(typeface,1) }
        val calculate=Button(this).apply { text="Calculate & Save"; isAllCaps=false; setOnClickListener {
            val a=obtained.text.toString().toDoubleOrNull(); val b=total.text.toString().toDoubleOrNull()
            if(a==null||b==null||b<=0||a<0||a>b) { result.text="Enter valid marks"; return@setOnClickListener }
            val p=a/b*100; val g=when { p>=90->"A+";p>=80->"A";p>=70->"B";p>=60->"C";p>=45->"D";p>=33->"E";else->"F" }
            result.text="%.2f%%   •   Grade %s".format(p,g)
            results.save(a,b,p,g) { _,message -> Toast.makeText(this@MainActivity,message,Toast.LENGTH_SHORT).show() }
        } }
        box.addView(obtained);box.addView(total);box.addView(calculate);box.addView(result);c.addView(box);root.addView(c)
        root.addView(TextView(this).apply { text="Quick Access\n\n🖼 Image Tools     📄 PDF Tools\n\n🏫 Join School      🕘 Result History"; textSize=17f; setPadding(0,dp(20),0,dp(20)) })
        content.addView(ScrollView(this).apply { addView(root) })
    }

    private fun showHistory() {
        content.removeAllViews(); val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        root.addView(TextView(this).apply { text="Result History"; textSize=25f; setTypeface(typeface,1) })
        val status=TextView(this).apply { text="Loading…"; setPadding(0,dp(12),0,dp(12)) }; root.addView(status)
        results.history({ rows -> status.text=if(rows.isEmpty()) "No saved results yet" else "${rows.size} saved result(s)"
            rows.forEach { r -> val c=card(); c.addView(TextView(this).apply { text="${r["percentage"]}%  •  Grade ${r["grade"]}\n${r["obtained"]} / ${r["total"]}"; textSize=17f }); val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));root.addView(c,lp) }
        }, { status.text=it })
        content.addView(ScrollView(this).apply { addView(root) })
    }

    private fun showTools() { content.removeAllViews(); val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; root.addView(TextView(this).apply { text="Student Tools";textSize=25f;setTypeface(typeface,1) }); arrayOf("Image Resize","Image Compress","Image Merge","Background Remover","PDF Tools","Image ↔ PDF").forEach { n -> val c=card();c.addView(TextView(this).apply { text=n;textSize=17f;setTypeface(typeface,1) });val lp=LinearLayout.LayoutParams(-1,dp(68));lp.setMargins(0,0,0,dp(10));root.addView(c,lp) };content.addView(ScrollView(this).apply { addView(root) }) }
}
