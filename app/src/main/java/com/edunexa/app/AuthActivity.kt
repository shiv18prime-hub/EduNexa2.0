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
    private var selectedRole = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val welcome = findViewById<LinearLayout>(R.id.welcomePanel)
        val form = findViewById<LinearLayout>(R.id.formPanel)
        val title = findViewById<TextView>(R.id.formTitle)
        val subtitle = findViewById<TextView>(R.id.formSubtitle)
        val name = findViewById<EditText>(R.id.nameInput)
        val email = findViewById<EditText>(R.id.emailInput)
        val password = findViewById<EditText>(R.id.passwordInput)
        val school = findViewById<EditText>(R.id.schoolInput)
        val role = findViewById<Spinner>(R.id.roleSpinner)
        val register = findViewById<Button>(R.id.registerBtn)
        val login = findViewById<Button>(R.id.loginBtn)
        val status = findViewById<TextView>(R.id.statusText)
        val progress = findViewById<ProgressBar>(R.id.progress)
        role.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Student", "School"))

        fun resetFields() {
            name.text.clear(); email.text.clear(); password.text.clear(); school.text.clear(); status.text = ""
        }
        fun showSignup(roleName: String) {
            resetFields(); selectedRole = roleName
            welcome.visibility = View.GONE; form.visibility = View.VISIBLE
            name.visibility = View.VISIBLE; register.visibility = View.VISIBLE; login.visibility = View.GONE
            school.visibility = if (roleName == "school") View.VISIBLE else View.GONE
            role.setSelection(if (roleName == "school") 1 else 0)
            title.text = if (roleName == "school") "Create School Account" else "Create Student Account"
            subtitle.text = if (roleName == "school") "Register your school • Admin approval required" else "Learn • Calculate • Connect • Grow"
        }
        fun showLogin() {
            resetFields(); welcome.visibility = View.GONE; form.visibility = View.VISIBLE
            name.visibility = View.GONE; school.visibility = View.GONE; register.visibility = View.GONE; login.visibility = View.VISIBLE
            title.text = "Welcome Back"
            subtitle.text = "Login with your EduNexa email account"
        }
        fun showWelcome() {
            resetFields(); progress.visibility = View.GONE; form.visibility = View.GONE; welcome.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.studentChoiceBtn).setOnClickListener { showSignup("student") }
        findViewById<Button>(R.id.schoolChoiceBtn).setOnClickListener { showSignup("school") }
        findViewById<Button>(R.id.showLoginBtn).setOnClickListener { showLogin() }
        findViewById<Button>(R.id.backBtn).setOnClickListener { showWelcome() }

        register.setOnClickListener {
            if (name.text.isBlank() || email.text.isBlank() || password.text.length < 6 || (selectedRole == "school" && school.text.isBlank())) {
                status.text = "Please complete all required fields. Password must be at least 6 characters."
                return@setOnClickListener
            }
            progress.visibility = View.VISIBLE
            authRepository.register(name.text.toString().trim(), email.text.toString().trim(), password.text.toString(), selectedRole, school.text.toString().trim()) { ok, message ->
                progress.visibility = View.GONE; status.text = message
                if (ok) routeCurrentUser(status, progress)
            }
        }

        login.setOnClickListener {
            if (email.text.isBlank() || password.text.isBlank()) {
                status.text = "Enter email and password"
                return@setOnClickListener
            }
            progress.visibility = View.VISIBLE
            authRepository.login(email.text.toString().trim(), password.text.toString()) { ok, message ->
                status.text = message
                if (ok) routeCurrentUser(status, progress) else progress.visibility = View.GONE
            }
        }
    }

    private fun routeCurrentUser(status: TextView, progress: ProgressBar) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run { progress.visibility = View.GONE; return }
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            progress.visibility = View.GONE
            when (doc.getString("role")) {
                "admin" -> open(AdminActivity::class.java)
                "school" -> if (doc.getBoolean("schoolApproved") == true) open(SchoolActivity::class.java) else open(PendingApprovalActivity::class.java)
                "student" -> open(MainActivity::class.java)
                else -> { status.text = "Account role is not configured"; FirebaseAuth.getInstance().signOut() }
            }
        }.addOnFailureListener { progress.visibility = View.GONE; status.text = it.message ?: "Could not load account" }
    }

    private fun open(target: Class<*>) { startActivity(Intent(this, target)); finish() }
}
