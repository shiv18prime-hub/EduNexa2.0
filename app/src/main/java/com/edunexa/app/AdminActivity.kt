package com.edunexa.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.AdminRepository
import com.edunexa.app.data.UserProfile

class AdminActivity : AppCompatActivity() {
    private val repo = AdminRepository()
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        list = findViewById(R.id.schoolList)
        status = findViewById(R.id.adminStatus)
        progress = findViewById(R.id.adminProgress)
        loadSchools()
    }

    private fun loadSchools() {
        progress.visibility = View.VISIBLE
        repo.pendingSchools({ schools ->
            progress.visibility = View.GONE
            list.removeAllViews()
            status.text = if (schools.isEmpty()) "No pending school registrations" else "${schools.size} school(s) awaiting review"
            schools.forEach { addSchoolCard(it) }
        }, { error -> progress.visibility = View.GONE; status.text = error })
    }

    private fun addSchoolCard(school: UserProfile) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }
        card.addView(TextView(this).apply { text = school.schoolName.ifBlank { school.name }; textSize = 20f })
        card.addView(TextView(this).apply { text = "Contact: ${school.name}\n${school.email}"; textSize = 15f })
        val actions = LinearLayout(this)
        val approve = Button(this).apply { text = "Approve"; setOnClickListener { decide(school.uid, true) } }
        val reject = Button(this).apply { text = "Reject"; setOnClickListener { decide(school.uid, false) } }
        actions.addView(approve); actions.addView(reject)
        card.addView(actions)
        list.addView(card)
    }

    private fun decide(uid: String, approved: Boolean) {
        repo.setSchoolApproval(uid, approved) { ok, message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            if (ok) loadSchools()
        }
    }
}
