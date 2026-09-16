package com.edunexa.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.AuthRepository
import com.edunexa.app.data.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class AuthActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private var selectedRole = "student"
    private var routingSession = false

    private lateinit var name: EditText
    private lateinit var school: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var classInput: EditText
    private lateinit var contact: EditText
    private lateinit var address: EditText
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            progress.visibility = View.VISIBLE
            firebaseAuth.signInWithCredential(credential).addOnSuccessListener { authResult ->
                val user = authResult.user ?: return@addOnSuccessListener
                val isSchool = selectedRole == "school"
                val profile = UserProfile(
                    uid = user.uid,
                    name = if (isSchool) school.text.toString().trim() else (name.text.toString().trim().ifBlank { user.displayName ?: "Student" }),
                    email = user.email ?: "",
                    role = selectedRole,
                    schoolName = if (isSchool) school.text.toString().trim() else "",
                    className = if (isSchool) "" else classInput.text.toString().trim(),
                    contactNumber = if (isSchool) contact.text.toString().trim() else "",
                    address = if (isSchool) address.text.toString().trim() else "",
                    schoolApproved = !isSchool
                )
                db.collection("users").document(user.uid).set(profile).addOnSuccessListener {
                    routeCurrentUser(status, progress)
                }.addOnFailureListener {
                    progress.visibility = View.GONE
                    status.text = it.message ?: "Could not save profile"
                }
            }.addOnFailureListener {
                progress.visibility = View.GONE
                status.text = it.message ?: "Google sign-in failed"
            }
        } catch (e: Exception) {
            progress.visibility = View.GONE
            status.text = e.message ?: "Google sign-in cancelled"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        val welcome=findViewById<LinearLayout>(R.id.welcomePanel); val form=findViewById<LinearLayout>(R.id.formPanel)
        val header=findViewById<ImageView>(R.id.formHeaderImage); val title=findViewById<TextView>(R.id.formTitle); val subtitle=findViewById<TextView>(R.id.formSubtitle)
        name=findViewById(R.id.nameInput); school=findViewById(R.id.schoolInput); email=findViewById(R.id.emailInput); password=findViewById(R.id.passwordInput)
        classInput=findViewById(R.id.classInput); contact=findViewById(R.id.contactInput); address=findViewById(R.id.addressInput)
        val register=findViewById<TextView>(R.id.registerBtn); val login=findViewById<TextView>(R.id.loginBtn); val approval=findViewById<TextView>(R.id.approvalInfo)
        val social=findViewById<LinearLayout>(R.id.socialOptions); status=findViewById(R.id.statusText); progress=findViewById(R.id.progress)

        // Firebase Auth persists a valid session locally. Route it before showing login again.
        if (firebaseAuth.currentUser != null) {
            welcome.visibility = View.GONE
            form.visibility = View.GONE
            progress.visibility = View.VISIBLE
            status.text = "Restoring your session..."
            routeCurrentUser(status, progress)
        }

        fun reset(){ listOf(name,school,email,password,classInput,contact,address).forEach{it.text.clear()}; status.text="" }
        fun signup(role:String){ reset(); selectedRole=role; welcome.visibility=View.GONE; form.visibility=View.VISIBLE; register.visibility=View.VISIBLE; login.visibility=View.GONE; social.visibility=View.VISIBLE
            val isSchool=role=="school"; header.setImageResource(if(isSchool) R.drawable.school_art else R.drawable.student_art)
            title.text=if(isSchool) "Create School Account" else "Create Student Account"; subtitle.text=if(isSchool) "Register your school to manage students and academic activities" else "Start your learning journey with EduNexa"
            name.visibility=if(isSchool) View.GONE else View.VISIBLE; school.visibility=if(isSchool) View.VISIBLE else View.GONE; classInput.visibility=if(isSchool) View.GONE else View.VISIBLE
            contact.visibility=if(isSchool) View.VISIBLE else View.GONE; address.visibility=if(isSchool) View.VISIBLE else View.GONE; approval.visibility=if(isSchool) View.VISIBLE else View.GONE }
        fun showLogin(){ reset(); welcome.visibility=View.GONE; form.visibility=View.VISIBLE; header.setImageResource(R.drawable.edunexa_logo); title.text="Welcome Back"; subtitle.text="Login with your EduNexa email account"
            listOf(name,school,classInput,contact,address,register,approval,social).forEach{it.visibility=View.GONE}; login.visibility=View.VISIBLE }
        fun showWelcome(){ reset(); progress.visibility=View.GONE; form.visibility=View.GONE; welcome.visibility=View.VISIBLE }

        findViewById<View>(R.id.studentChoiceBtn).setOnClickListener{signup("student")}; findViewById<View>(R.id.schoolChoiceBtn).setOnClickListener{signup("school")}
        findViewById<View>(R.id.showLoginBtn).setOnClickListener{showLogin()}; findViewById<View>(R.id.loginLink).setOnClickListener{showLogin()}; findViewById<View>(R.id.backBtn).setOnClickListener{showWelcome()}
        findViewById<View>(R.id.emailContinueBtn).setOnClickListener { email.requestFocus() }
        findViewById<View>(R.id.googleBtn).setOnClickListener {
            val isSchool=selectedRole=="school"
            val extraOk=if(isSchool) school.text.isNotBlank()&&contact.text.isNotBlank()&&address.text.isNotBlank() else classInput.text.isNotBlank()
            if(!extraOk){ status.text=if(isSchool) "Enter school name, contact number and address first" else "Enter your class first"; return@setOnClickListener }
            val options=GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
            googleLauncher.launch(GoogleSignIn.getClient(this,options).signInIntent)
        }

        register.setOnClickListener{
            val isSchool=selectedRole=="school"
            val requiredOk=email.text.isNotBlank() && password.text.length>=6 && if(isSchool) school.text.isNotBlank()&&contact.text.isNotBlank()&&address.text.isNotBlank() else name.text.isNotBlank()&&classInput.text.isNotBlank()
            if(!requiredOk){status.text="Please complete all required fields. Password must be at least 6 characters."; return@setOnClickListener}
            progress.visibility=View.VISIBLE
            authRepository.register(name=if(isSchool) school.text.toString().trim() else name.text.toString().trim(), email=email.text.toString().trim(), password=password.text.toString(), role=selectedRole, schoolName=if(isSchool) school.text.toString().trim() else "", className=classInput.text.toString().trim(), contactNumber=contact.text.toString().trim(), address=address.text.toString().trim()){ok,msg->progress.visibility=View.GONE;status.text=msg;if(ok)routeCurrentUser(status,progress)}
        }
        login.setOnClickListener{ if(email.text.isBlank()||password.text.isBlank()){status.text="Enter email and password";return@setOnClickListener};progress.visibility=View.VISIBLE;authRepository.login(email.text.toString().trim(),password.text.toString()){ok,msg->status.text=msg;if(ok)routeCurrentUser(status,progress)else progress.visibility=View.GONE} }
    }

    override fun onStart() {
        super.onStart()
        if (::status.isInitialized && firebaseAuth.currentUser != null && !routingSession) {
            routeCurrentUser(status, progress)
        }
    }

    private fun routeCurrentUser(status:TextView,progress:ProgressBar){
        if (routingSession) return
        val uid=FirebaseAuth.getInstance().currentUser?.uid?:run{progress.visibility=View.GONE;return}
        routingSession=true
        progress.visibility=View.VISIBLE
        db.collection("users").document(uid).get().addOnSuccessListener{doc->
            routingSession=false
            progress.visibility=View.GONE
            when(doc.getString("role")){
                "admin"->open(AdminActivity::class.java)
                "school"->if(doc.getBoolean("schoolApproved")==true)open(SchoolActivity::class.java)else open(PendingApprovalActivity::class.java)
                "student"->open(MainActivity::class.java)
                else->{status.text="Account role is not configured";FirebaseAuth.getInstance().signOut()}
            }
        }.addOnFailureListener{
            routingSession=false
            progress.visibility=View.GONE
            status.text=it.message?:"Could not restore account. Check your internet and try again."
        }
    }

    private fun open(target:Class<*>){
        startActivity(Intent(this,target).apply { flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
        finish()
    }
}
