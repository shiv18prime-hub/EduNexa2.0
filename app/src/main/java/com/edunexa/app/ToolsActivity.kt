package com.edunexa.app
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
class ToolsActivity:AppCompatActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(36,36,36,36)};r.addView(TextView(this).apply{text="EduNexa Tools";textSize=26f;setTypeface(typeface,1)});r.addView(TextView(this).apply{text="Student & Cyber-Café Toolkit";setPadding(0,8,0,20)});fun add(t:String,cls:Class<*>){r.addView(Button(this).apply{text=t;setOnClickListener{startActivity(Intent(this@ToolsActivity,cls))}})};add("Exam Form Photo & Signature Maker",PhotoSignatureToolActivity::class.java);add("Target KB Compressor",TargetKbToolActivity::class.java);add("PDF Toolkit",PdfToolkitActivity::class.java);setContentView(ScrollView(this).apply{addView(r)})}}
