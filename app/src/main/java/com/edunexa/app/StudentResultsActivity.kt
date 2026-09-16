package com.edunexa.app
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.SchoolResultRepository
import com.google.android.material.card.MaterialCardView
class StudentResultsActivity:AppCompatActivity(){private val repo=SchoolResultRepository();private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt();override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(18))};setContentView(ScrollView(this).apply{addView(root)});root.addView(TextView(this).apply{text="School Results";textSize=27f;setTypeface(typeface,1)});val status=TextView(this).apply{text="Loading…";setPadding(0,dp(8),0,dp(14))};root.addView(status);repo.myResults({rows->status.text=if(rows.isEmpty())"No published results yet" else "${rows.size} published subject result(s)";rows.forEach{r->val c=MaterialCardView(this).apply{radius=dp(18).toFloat();cardElevation=dp(2).toFloat();setCardBackgroundColor(Color.WHITE);setContentPadding(dp(16),dp(16),dp(16),dp(16));addView(TextView(this@StudentResultsActivity).apply{text="${r["exam"]} • ${r["subject"]}\n${r["obtained"]} / ${r["total"]}\n${"%.2f".format((r["percentage"] as? Number)?.toDouble()?:0.0)}% • Grade ${r["grade"]}";textSize=17f})};val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(11));root.addView(c,lp)}},{status.text=it})}}
