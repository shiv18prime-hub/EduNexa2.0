package com.edunexa.app

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.McqRepository
import com.google.android.material.card.MaterialCardView

class McqHistoryActivity:AppCompatActivity(){private val repo=McqRepository();private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt();override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;padding=dp(18)};setContentView(ScrollView(this).apply{addView(root)});root.addView(TextView(this).apply{text="Test History";textSize=27f;setTypeface(typeface,1)});val status=TextView(this).apply{text="Loading…";setPadding(0,dp(8),0,dp(14))};root.addView(status);repo.attemptHistory({rows->status.text=if(rows.isEmpty())"No test attempts yet" else "${rows.size} completed test(s)";rows.forEach{r->val score=(r["score"] as? Number)?.toInt()?:0;val total=(r["total"] as? Number)?.toInt()?:0;val pct=(r["percentage"] as? Number)?.toDouble()?:0.0;val card=MaterialCardView(this).apply{radius=dp(18).toFloat();cardElevation=dp(3).toFloat();setCardBackgroundColor(Color.WHITE);setContentPadding(dp(16),dp(16),dp(16),dp(16));addView(TextView(this@McqHistoryActivity).apply{text="Score  $score / $total\nPercentage  %.1f%%\nTest ID  ${r["testId"]}".format(pct);textSize=17f})};val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(12));root.addView(card,lp)}},{status.text=it})}}
