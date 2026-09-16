package com.edunexa.app
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
class PhotoSignatureToolActivity:AppCompatActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(36,36,36,36)};r.addView(TextView(this).apply{text="Exam Form Photo & Signature Maker";textSize=24f;setTypeface(typeface,1)});r.addView(TextView(this).apply{text="Photo presets: passport/form photo\nSignature presets: exam/job/admission forms\nCustom width, height and target KB supported";textSize=16f;setPadding(0,20,0,20)});r.addView(Button(this).apply{text="Select Photo / Signature";setOnClickListener{Toast.makeText(this@PhotoSignatureToolActivity,"File picker integration next",Toast.LENGTH_SHORT).show()}});setContentView(r)}}
