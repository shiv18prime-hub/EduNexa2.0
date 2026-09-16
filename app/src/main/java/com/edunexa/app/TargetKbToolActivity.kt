package com.edunexa.app
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
class TargetKbToolActivity:AppCompatActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(36,36,36,36)};r.addView(TextView(this).apply{text="Target KB Compressor";textSize=24f;setTypeface(typeface,1)});val size=EditText(this).apply{hint="Target size in KB (e.g. 20, 50, 100)";inputType=2};r.addView(size);r.addView(Button(this).apply{text="Select Image";setOnClickListener{Toast.makeText(this@TargetKbToolActivity,"Image picker + compression engine next",Toast.LENGTH_SHORT).show()}});setContentView(r)}}
