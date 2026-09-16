package com.edunexa.app
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class StudentManagementActivity:AppCompatActivity(){private val db=FirebaseFirestore.getInstance();private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt();override fun onCreate(b:Bundle?){super.onCreate(b);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;padding=dp(18)};setContentView(ScrollView(this).apply{addView(root)});root.addView(TextView(this).apply{text="Student Management";textSize=27f;setTypeface(typeface,1)});val status=TextView(this).apply{text="Loading connected students…";setPadding(0,dp(8),0,dp(14))};root.addView(status);val sid=FirebaseAuth.getInstance().currentUser?.uid?:return;db.collection("users").whereEqualTo("role","student").whereEqualTo("schoolId",sid).get().addOnSuccessListener{s->status.text=if(s.isEmpty)"No connected students" else "${s.size()} connected student(s)";s.documents.forEach{d->val c=MaterialCardView(this).apply{radius=dp(18).toFloat();cardElevation=dp(2).toFloat();setCardBackgroundColor(Color.WHITE);setContentPadding(dp(16),dp(16),dp(16),dp(16));addView(TextView(this@StudentManagementActivity).apply{text="${d.getString("name")?:"Student"}\n${d.getString("email")?:""}\nClass: ${d.getString("className")?:"Not set"}\nSchool Code: ${d.getString("schoolCode")?:"—"}";textSize=16f})};val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));root.addView(c,lp)}}.addOnFailureListener{status.text=it.message?:"Could not load students"}}}
