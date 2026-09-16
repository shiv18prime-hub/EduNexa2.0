package com.edunexa.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        val name=findViewById<EditText>(R.id.nameInput); val email=findViewById<EditText>(R.id.emailInput)
        val password=findViewById<EditText>(R.id.passwordInput); val school=findViewById<EditText>(R.id.schoolInput)
        val role=findViewById<Spinner>(R.id.roleSpinner); val status=findViewById<TextView>(R.id.statusText); val progress=findViewById<ProgressBar>(R.id.progress)
        role.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("Student","School"))

        findViewById<Button>(R.id.registerBtn).setOnClickListener {
            val selectedRole=if(role.selectedItem.toString()=="School") "school" else "student"
            if(name.text.isBlank()||email.text.isBlank()||password.text.length<6||(selectedRole=="school"&&school.text.isBlank())) { status.text="Please complete all required fields. Password must be at least 6 characters."; return@setOnClickListener }
            progress.visibility=View.VISIBLE
            authRepository.register(name.text.toString().trim(),email.text.toString().trim(),password.text.toString(),selectedRole,school.text.toString().trim()) { ok,message ->
                progress.visibility=View.GONE; status.text=message
                if(ok) routeCurrentUser(status,progress)
            }
        }

        findViewById<Button>(R.id.loginBtn).setOnClickListener {
            if(email.text.isBlank()||password.text.isBlank()) { status.text="Enter email and password"; return@setOnClickListener }
            progress.visibility=View.VISIBLE
            authRepository.login(email.text.toString().trim(),password.text.toString()) { ok,message ->
                status.text=message
                if(ok) routeCurrentUser(status,progress) else progress.visibility=View.GONE
            }
        }
    }

    private fun routeCurrentUser(status:TextView, progress:ProgressBar) {
        val uid=FirebaseAuth.getInstance().currentUser?.uid ?: run { progress.visibility=View.GONE; return }
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            progress.visibility=View.GONE
            when(doc.getString("role")) {
                "admin" -> open(AdminActivity::class.java)
                "school" -> if(doc.getBoolean("schoolApproved")==true) open(SchoolActivity::class.java) else open(PendingApprovalActivity::class.java)
                "student" -> open(MainActivity::class.java)
                else -> { status.text="Account role is not configured"; FirebaseAuth.getInstance().signOut() }
            }
        }.addOnFailureListener { progress.visibility=View.GONE; status.text=it.message ?: "Could not load account" }
    }

    private fun open(target:Class<*>) { startActivity(Intent(this,target)); finish() }
}
