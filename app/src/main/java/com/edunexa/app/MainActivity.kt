package com.edunexa.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity:AppCompatActivity(){
 private lateinit var content:FrameLayout
 private val db=FirebaseFirestore.getInstance()
 private var profileImage:ImageView?=null
 private val photoPicker=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{try{contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};getSharedPreferences("edunexa",MODE_PRIVATE).edit().putString("profilePhoto",it.toString()).apply();profileImage?.setImageURI(it);Toast.makeText(this,"Profile photo updated",Toast.LENGTH_SHORT).show()}}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun card()=MaterialCardView(this).apply{radius=dp(22).toFloat();cardElevation=dp(3).toFloat();setCardBackgroundColor(Color.WHITE);setContentPadding(dp(18),dp(18),dp(18),dp(18))}
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main);content=findViewById(R.id.content);findViewById<Button>(R.id.homeBtn).setOnClickListener{showHome()};findViewById<Button>(R.id.toolsBtn).setOnClickListener{startActivity(Intent(this,ToolsActivity::class.java))};findViewById<Button>(R.id.connectBtn).setOnClickListener{startActivity(Intent(this,JoinSchoolActivity::class.java))};findViewById<Button>(R.id.profileBtn).setOnClickListener{showProfile()};showHome()}
 private fun action(r:LinearLayout,t:String,s:String,g:()->Unit){val c=card();val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(TextView(this).apply{text=t;textSize=18f;setTypeface(typeface,1)});b.addView(TextView(this).apply{text=s;textSize=14f;setTextColor(Color.GRAY)});c.addView(b);c.setOnClickListener{g()};r.addView(c,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(11))})}
 private fun showHome(){content.removeAllViews();val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(4),dp(4),dp(4),dp(20))};r.addView(TextView(this).apply{text="Hi, Student 👋";textSize=25f;setTypeface(typeface,1)});r.addView(TextView(this).apply{text="Learn • Calculate • Connect • Grow";setTextColor(Color.GRAY)});action(r,"🧮 Result Calculator","Calculate percentage, grade and save history"){startActivity(Intent(this,ResultCalculatorActivity::class.java))};action(r,"📝 MCQ Tests","Take school tests"){startActivity(Intent(this,StudentMcqActivity::class.java))};action(r,"📊 School Results","Published school results"){startActivity(Intent(this,StudentResultsActivity::class.java))};action(r,"📅 My Attendance","Attendance history"){startActivity(Intent(this,StudentAttendanceActivity::class.java))};action(r,"📢 Notices","School announcements"){startActivity(Intent(this,NoticeActivity::class.java))};action(r,"🏫 Connect School","Join with School Code"){startActivity(Intent(this,JoinSchoolActivity::class.java))};content.addView(ScrollView(this).apply{addView(r)})}
 private fun showProfile(){content.removeAllViews();val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(8),dp(12),dp(8),dp(28))};r.addView(TextView(this).apply{text="My Profile";textSize=26f;setTypeface(typeface,1)});profileImage=ImageView(this).apply{setImageResource(R.drawable.edunexa_logo);scaleType=ImageView.ScaleType.CENTER_CROP};r.addView(profileImage,LinearLayout.LayoutParams(dp(112),dp(112)).apply{setMargins(0,dp(18),0,dp(8))});getSharedPreferences("edunexa",MODE_PRIVATE).getString("profilePhoto",null)?.let{runCatching{profileImage?.setImageURI(Uri.parse(it))}};r.addView(Button(this).apply{text="Change Profile Photo";isAllCaps=false;setOnClickListener{photoPicker.launch(arrayOf("image/*"))}});val info=TextView(this).apply{text="Loading account…";textSize=16f;setPadding(0,dp(18),0,dp(18))};r.addView(info);FirebaseAuth.getInstance().currentUser?.uid?.let{uid->db.collection("users").document(uid).get().addOnSuccessListener{d->info.text="${d.getString("name")?:"Student"}\n${d.getString("email")?:FirebaseAuth.getInstance().currentUser?.email.orEmpty()}\nClass: ${d.getString("className")?:"—"}"}};action(r,"🏆 MCQ History","Completed scores"){startActivity(Intent(this,McqHistoryActivity::class.java))};action(r,"🛠 EduNexa Tools","Photo, KB and PDF utilities"){startActivity(Intent(this,ToolsActivity::class.java))};r.addView(Button(this).apply{text="Logout";isAllCaps=false;setOnClickListener{FirebaseAuth.getInstance().signOut();startActivity(Intent(this@MainActivity,AuthActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish()}});content.addView(ScrollView(this).apply{addView(r)})}
}
