package com.edunexa.app

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.SchoolResultRepository
import com.google.android.material.card.MaterialCardView

class StudentResultsActivity : AppCompatActivity() {
 private val repo=SchoolResultRepository();private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(18))};setContentView(ScrollView(this).apply{addView(root)});root.addView(TextView(this).apply{text="My Performance";textSize=27f;setTypeface(typeface,1);setTextColor(Color.parseColor("#20243A"))});val status=TextView(this).apply{text="Loading published results…";setPadding(0,dp(8),0,dp(14))};root.addView(status)
 repo.myResults({rows->if(rows.isEmpty()){status.text="No published results yet";return@myResults};val percentages=rows.map{(it["percentage"] as? Number)?.toDouble()?:0.0};val avg=percentages.average();val latest=percentages.first();val previous=percentages.drop(1).firstOrNull();val trend=previous?.let{latest-it};val summary=MaterialCardView(this).apply{radius=dp(20).toFloat();cardElevation=dp(2).toFloat();setCardBackgroundColor(Color.parseColor("#F4F3FF"));setContentPadding(dp(17),dp(16),dp(17),dp(16));addView(TextView(this@StudentResultsActivity).apply{text="Overall Average  ${"%.1f".format(avg)}%\nLatest  ${"%.1f".format(latest)}%${trend?.let{"  •  ${if(it>=0)"↑" else "↓"} ${"%.1f".format(kotlin.math.abs(it))}% vs previous"}?:""}\n${if(avg>=33)"Overall status: Passing range" else "Overall status: Needs improvement"}";textSize=16f;setTypeface(typeface,1);setTextColor(Color.parseColor("#4338CA"))})};root.addView(summary,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(14))});status.text="${rows.size} published subject result(s) • Track improvement below";rows.forEach{r->val p=(r["percentage"] as? Number)?.toDouble()?:0.0;val c=MaterialCardView(this).apply{radius=dp(18).toFloat();cardElevation=dp(2).toFloat();setCardBackgroundColor(Color.WHITE);setContentPadding(dp(16),dp(16),dp(16),dp(16));addView(TextView(this@StudentResultsActivity).apply{text="${r["exam"]} • ${r["subject"]}\n${r["obtained"]} / ${r["total"]}\n${"%.2f".format(p)}% • Grade ${r["grade"]} • ${if(p>=33)"PASS" else "NEEDS IMPROVEMENT"}";textSize=17f})};root.addView(c,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(11))})}},{status.text=it})}
}
