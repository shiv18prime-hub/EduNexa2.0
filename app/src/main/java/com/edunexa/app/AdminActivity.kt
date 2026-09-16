package com.edunexa.app

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.AdminRepository
import com.edunexa.app.data.UserProfile
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {
    private val repo = AdminRepository()
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_admin)
        list=findViewById(R.id.schoolList); status=findViewById(R.id.adminStatus); progress=findViewById(R.id.adminProgress)
        findViewById<Button>(R.id.pendingTab).setOnClickListener { loadSchools() }
        findViewById<Button>(R.id.usersTab).setOnClickListener { showInfo("User Management", "Student and school account controls") }
        findViewById<Button>(R.id.couponsTab).setOnClickListener { showInfo("Coupon Management", "Create and manage Pro discount codes") }
        findViewById<Button>(R.id.proTab).setOnClickListener { showInfo("EduNexa Pro", "Subscriptions and Pro access controls") }
        verifyAdmin()
    }

    private fun verifyAdmin() {
        progress.visibility=View.VISIBLE
        val uid=FirebaseAuth.getInstance().currentUser?.uid ?: return finish()
        FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener { doc ->
            if(doc.getString("role")!="admin") { Toast.makeText(this,"Admin access required",Toast.LENGTH_LONG).show(); finish() }
            else loadSchools()
        }.addOnFailureListener { Toast.makeText(this,"Unable to verify admin",Toast.LENGTH_LONG).show(); finish() }
    }

    private fun loadSchools() {
        progress.visibility=View.VISIBLE; list.removeAllViews()
        repo.pendingSchools({ schools ->
            progress.visibility=View.GONE
            status.text=if(schools.isEmpty()) "No pending school registrations" else "${schools.size} school(s) awaiting review"
            schools.forEach { addSchoolCard(it) }
        }, { error -> progress.visibility=View.GONE; status.text=error })
    }

    private fun addSchoolCard(school: UserProfile) {
        val card=MaterialCardView(this).apply { radius=dp(20).toFloat(); cardElevation=dp(3).toFloat(); setCardBackgroundColor(Color.WHITE); setContentPadding(dp(18),dp(18),dp(18),dp(18)) }
        val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        body.addView(TextView(this).apply { text=school.schoolName.ifBlank { school.name }; textSize=20f; setTypeface(typeface,1); setTextColor(Color.rgb(38,43,68)) })
        body.addView(TextView(this).apply { text="Contact: ${school.name}\n${school.email}"; textSize=14f; setTextColor(Color.rgb(95,102,124)); setPadding(0,dp(7),0,dp(10)) })
        val actions=LinearLayout(this)
        actions.addView(Button(this).apply { text="Approve"; isAllCaps=false; setOnClickListener { decide(school.uid,true) } })
        actions.addView(Button(this).apply { text="Reject"; isAllCaps=false; setOnClickListener { decide(school.uid,false) } })
        body.addView(actions); card.addView(body)
        val lp=LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,dp(12)); list.addView(card,lp)
    }

    private fun showInfo(title:String, text:String) {
        progress.visibility=View.GONE; list.removeAllViews(); status.text=title
        val card=MaterialCardView(this).apply { radius=dp(20).toFloat(); setCardBackgroundColor(Color.WHITE); setContentPadding(dp(20),dp(24),dp(20),dp(24)) }
        card.addView(TextView(this).apply { this.text="$title\n\n$text"; textSize=18f; setTextColor(Color.rgb(45,50,75)) }); list.addView(card)
    }

    private fun decide(uid:String, approved:Boolean) { repo.setSchoolApproval(uid,approved) { ok,message -> Toast.makeText(this,message,Toast.LENGTH_SHORT).show(); if(ok) loadSchools() } }
}
