package com.edunexa.app
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
class PdfToolkitActivity:AppCompatActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(36,36,36,36)};r.addView(TextView(this).apply{text="PDF Toolkit";textSize=24f;setTypeface(typeface,1)});arrayOf("Images → PDF","PDF → Images","Merge PDFs","Split PDF","Compress PDF","Reorder / Delete Pages").forEach{n->r.addView(Button(this).apply{text=n;setOnClickListener{Toast.makeText(this@PdfToolkitActivity,"$n processing integration next",Toast.LENGTH_SHORT).show()}})};setContentView(ScrollView(this).apply{addView(r)})}}
