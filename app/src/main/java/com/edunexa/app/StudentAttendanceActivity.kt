package com.edunexa.app

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.AttendanceRepository
import com.google.android.material.card.MaterialCardView

class StudentAttendanceActivity:AppCompatActivity(){private val repo=AttendanceRepository();private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt();override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;padding=dp(18)};setContentView(ScrollView(this).apply{addView(root)});root.addView(TextView(this).apply{text="My Attendance";textSize=27f;setTypeface(typeface,1)});val summary=TextView(this).apply{setPadding(0,dp(8),0,dp(14))};root.addView(summary);repo.studentAttendance({rows->val present=rows.count{it["present"]==true};val pct=if(rows.isNotEmpty())present*100.0/rows.size else 0.0;summary.text=if(rows.isEmpty())"No attendance records yet" else "Present $present/${rows.size} • %.1f%%".format(pct);rows.forEach{r->val card=MaterialCardView(this).apply{radius=dp(16).toFloat();cardElevation=dp(2).toFloat();setCardBackgroundColor(Color.WHITE);setContentPadding(dp(15),dp(15),dp(15),dp(15));addView(TextView(this@StudentAttendanceActivity).apply{text="${r["date"]}   •   ${if(r["present"]==true)"Present ✓" else "Absent ✗"}";textSize=17f})};val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));root.addView(card,lp)}} ,{summary.text=it})}}
