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
        val welcome=findViewById<LinearLayout>(R.id.welcomePanel); val form=findViewById<LinearLayout>(R.id.formPanel)
        val header=findViewById<ImageView>(R.id.formHeaderImage); val title=findViewById<TextView>(R.id.formTitle); val subtitle=findViewById<TextView>(R.id.formSubtitle)
        val name=findViewById<EditText>(R.id.nameInput); val school=findViewById<EditText>(R.id.schoolInput); val email=findViewById<EditText>(R.id.emailInput); val password=findViewById<EditText>(R.id.passwordInput)
        val classInput=findViewById<EditText>(R.id.classInput); val contact=findViewById<EditText>(R.id.contactInput); val address=findViewById<EditText>(R.id.addressInput)
        val register=findViewById<TextView>(R.id.registerBtn); val login=findViewById<TextView>(R.id.loginBtn); val approval=findViewById<TextView>(R.id.approvalInfo)
        val status=findViewById<TextView>(R.id.statusText); val progress=findViewById<ProgressBar>(R.id.progress)

        fun reset(){ listOf(name,school,email,password,classInput,contact,address).forEach{it.text.clear()}; status.text="" }
        fun signup(role:String){ reset(); selectedRole=role; welcome.visibility=View.GONE; form.visibility=View.VISIBLE; register.visibility=View.VISIBLE; login.visibility=View.GONE
            val isSchool=role=="school"; header.setImageResource(if(isSchool) R.drawable.school_art else R.drawable.student_art)
            title.text=if(isSchool) "Create School Account" else "Create Student Account"; subtitle.text=if(isSchool) "Register your school to manage students and academic activities" else "Start your learning journey with EduNexa"
            name.visibility=if(isSchool) View.GONE else View.VISIBLE; school.visibility=if(isSchool) View.VISIBLE else View.GONE; classInput.visibility=if(isSchool) View.GONE else View.VISIBLE
            contact.visibility=if(isSchool) View.VISIBLE else View.GONE; address.visibility=if(isSchool) View.VISIBLE else View.GONE; approval.visibility=if(isSchool) View.VISIBLE else View.GONE }
        fun showLogin(){ reset(); welcome.visibility=View.GONE; form.visibility=View.VISIBLE; header.setImageResource(R.drawable.edunexa_logo); title.text="Welcome Back"; subtitle.text="Login with your EduNexa email account"
            listOf(name,school,classInput,contact,address,register,approval).forEach{it.visibility=View.GONE}; login.visibility=View.VISIBLE }
        fun showWelcome(){ reset(); progress.visibility=View.GONE; form.visibility=View.GONE; welcome.visibility=View.VISIBLE }

        findViewById<View>(R.id.studentChoiceBtn).setOnClickListener{signup("student")}; findViewById<View>(R.id.schoolChoiceBtn).setOnClickListener{signup("school")}
        findViewById<View>(R.id.showLoginBtn).setOnClickListener{showLogin()}; findViewById<View>(R.id.loginLink).setOnClickListener{showLogin()}; findViewById<View>(R.id.backBtn).setOnClickListener{showWelcome()}

        register.setOnClickListener{
            val isSchool=selectedRole=="school"
            val requiredOk=email.text.isNotBlank() && password.text.length>=6 && if(isSchool) school.text.isNotBlank()&&contact.text.isNotBlank()&&address.text.isNotBlank() else name.text.isNotBlank()&&classInput.text.isNotBlank()
            if(!requiredOk){status.text="Please complete all required fields. Password must be at least 6 characters."; return@setOnClickListener}
            progress.visibility=View.VISIBLE
            authRepository.register(name=if(isSchool) school.text.toString().trim() else name.text.toString().trim(), email=email.text.toString().trim(), password=password.text.toString(), role=selectedRole, schoolName=if(isSchool) school.text.toString().trim() else "", className=classInput.text.toString().trim(), contactNumber=contact.text.toString().trim(), address=address.text.toString().trim()){ok,msg->progress.visibility=View.GONE;status.text=msg;if(ok)routeCurrentUser(status,progress)}
        }
        login.setOnClickListener{ if(email.text.isBlank()||password.text.isBlank()){status.text="Enter email and password";return@setOnClickListener};progress.visibility=View.VISIBLE;authRepository.login(email.text.toString().trim(),password.text.toString()){ok,msg->status.text=msg;if(ok)routeCurrentUser(status,progress)else progress.visibility=View.GONE} }
    }
    private fun routeCurrentUser(status:TextView,progress:ProgressBar){ val uid=FirebaseAuth.getInstance().currentUser?.uid?:run{progress.visibility=View.GONE;return};db.collection("users").document(uid).get().addOnSuccessListener{doc->progress.visibility=View.GONE;when(doc.getString("role")){"admin"->open(AdminActivity::class.java);"school"->if(doc.getBoolean("schoolApproved")==true)open(SchoolActivity::class.java)else open(PendingApprovalActivity::class.java);"student"->open(MainActivity::class.java);else->{status.text="Account role is not configured";FirebaseAuth.getInstance().signOut()}}}.addOnFailureListener{progress.visibility=View.GONE;status.text=it.message?:"Could not load account"} }
    private fun open(target:Class<*>){startActivity(Intent(this,target));finish()}
}
