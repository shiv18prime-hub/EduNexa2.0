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
    private var googleLoginMode = false
    private var adminLoginMode = false
    private lateinit var name: EditText; private lateinit var school: EditText; private lateinit var email: EditText; private lateinit var password: EditText
    private lateinit var classInput: EditText; private lateinit var contact: EditText; private lateinit var address: EditText
    private lateinit var status: TextView; private lateinit var progress: ProgressBar

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            progress.visibility = View.VISIBLE
            firebaseAuth.signInWithCredential(credential).addOnSuccessListener { authResult ->
                val user = authResult.user ?: return@addOnSuccessListener
                if (googleLoginMode) {
                    db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                        if (doc.exists() && doc.getString("role") != null) routeCurrentUser(status, progress)
                        else { firebaseAuth.signOut(); progress.visibility = View.GONE; status.text = "No EduNexa account is linked to this Google account. Create an account first." }
                    }.addOnFailureListener { progress.visibility = View.GONE; status.text = it.message ?: "Could not check your EduNexa account" }
                    return@addOnSuccessListener
                }
                val isSchool = selectedRole == "school"
                val profile = UserProfile(uid=user.uid,name=if(isSchool) school.text.toString().trim() else name.text.toString().trim().ifBlank{user.displayName?:"Student"},email=user.email?:"",role=selectedRole,schoolName=if(isSchool)school.text.toString().trim() else "",className=if(isSchool)"" else classInput.text.toString().trim(),contactNumber=if(isSchool)contact.text.toString().trim() else "",address=if(isSchool)address.text.toString().trim() else "",schoolApproved=!isSchool)
                db.collection("users").document(user.uid).set(profile).addOnSuccessListener { routeCurrentUser(status, progress) }.addOnFailureListener { progress.visibility=View.GONE;status.text=it.message?:"Could not save profile" }
            }.addOnFailureListener { progress.visibility=View.GONE;status.text=it.message?:"Google sign-in failed" }
        } catch(e:Exception){ progress.visibility=View.GONE;status.text=e.message?:"Google sign-in cancelled" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_auth)
        val welcome=findViewById<LinearLayout>(R.id.welcomePanel);val form=findViewById<LinearLayout>(R.id.formPanel);val header=findViewById<ImageView>(R.id.formHeaderImage);val title=findViewById<TextView>(R.id.formTitle);val subtitle=findViewById<TextView>(R.id.formSubtitle)
        name=findViewById(R.id.nameInput);school=findViewById(R.id.schoolInput);email=findViewById(R.id.emailInput);password=findViewById(R.id.passwordInput);classInput=findViewById(R.id.classInput);contact=findViewById(R.id.contactInput);address=findViewById(R.id.addressInput)
        val register=findViewById<TextView>(R.id.registerBtn);val login=findViewById<TextView>(R.id.loginBtn);val approval=findViewById<TextView>(R.id.approvalInfo);val social=findViewById<LinearLayout>(R.id.socialOptions);status=findViewById(R.id.statusText);progress=findViewById(R.id.progress)
        if(firebaseAuth.currentUser!=null){welcome.visibility=View.GONE;form.visibility=View.GONE;progress.visibility=View.VISIBLE;status.text="Restoring your session...";routeCurrentUser(status,progress)}
        fun reset(){listOf(name,school,email,password,classInput,contact,address).forEach{it.text.clear()};status.text=""}
        fun signup(role:String){reset();adminLoginMode=false;selectedRole=role;googleLoginMode=false;welcome.visibility=View.GONE;form.visibility=View.VISIBLE;register.visibility=View.VISIBLE;login.visibility=View.GONE;social.visibility=View.VISIBLE;findViewById<View>(R.id.emailContinueBtn).visibility=View.VISIBLE;val isSchool=role=="school";header.setImageResource(if(isSchool)R.drawable.school_art else R.drawable.student_art);title.text=if(isSchool)"Create School Account" else "Create Student Account";subtitle.text=if(isSchool)"Register your school to manage students and academic activities" else "Start your learning journey with EduNexa";name.visibility=if(isSchool)View.GONE else View.VISIBLE;school.visibility=if(isSchool)View.VISIBLE else View.GONE;classInput.visibility=if(isSchool)View.GONE else View.VISIBLE;contact.visibility=if(isSchool)View.VISIBLE else View.GONE;address.visibility=if(isSchool)View.VISIBLE else View.GONE;approval.visibility=if(isSchool)View.VISIBLE else View.GONE}
        fun showLogin(){reset();adminLoginMode=false;googleLoginMode=true;welcome.visibility=View.GONE;form.visibility=View.VISIBLE;header.setImageResource(R.drawable.edunexa_logo);title.text="Welcome Back";subtitle.text="Student / School Login";listOf(name,school,classInput,contact,address,register,approval).forEach{it.visibility=View.GONE};login.visibility=View.VISIBLE;login.text="Login";social.visibility=View.VISIBLE;findViewById<View>(R.id.emailContinueBtn).visibility=View.GONE}
        fun showAdminLogin(){reset();adminLoginMode=true;googleLoginMode=false;welcome.visibility=View.GONE;form.visibility=View.VISIBLE;header.setImageResource(R.drawable.edunexa_logo);title.text="Admin Control Login";subtitle.text="Authorized EduNexa administrators only";listOf(name,school,classInput,contact,address,register,approval).forEach{it.visibility=View.GONE};login.visibility=View.VISIBLE;login.text="Secure Admin Login";social.visibility=View.GONE}
        fun showWelcome(){reset();adminLoginMode=false;googleLoginMode=false;progress.visibility=View.GONE;form.visibility=View.GONE;welcome.visibility=View.VISIBLE}
        findViewById<View>(R.id.studentChoiceBtn).setOnClickListener{signup("student")};findViewById<View>(R.id.schoolChoiceBtn).setOnClickListener{signup("school")};findViewById<View>(R.id.showLoginBtn).setOnClickListener{showLogin()};findViewById<View>(R.id.adminLoginBtn).setOnClickListener{showAdminLogin()};findViewById<View>(R.id.loginLink).setOnClickListener{showLogin()};findViewById<View>(R.id.backBtn).setOnClickListener{showWelcome()};findViewById<View>(R.id.emailContinueBtn).setOnClickListener{email.requestFocus()}
        findViewById<View>(R.id.googleBtn).setOnClickListener{
            if(!googleLoginMode){val isSchool=selectedRole=="school";val ok=if(isSchool)school.text.isNotBlank()&&contact.text.isNotBlank()&&address.text.isNotBlank() else classInput.text.isNotBlank();if(!ok){status.text=if(isSchool)"Enter school name, contact number and address first" else "Enter your class first";return@setOnClickListener}}
            status.text="Choose a Google account...";val options=GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();val client=GoogleSignIn.getClient(this,options);client.signOut().addOnCompleteListener{googleLauncher.launch(client.signInIntent)}
        }
        register.setOnClickListener{val isSchool=selectedRole=="school";val ok=email.text.isNotBlank()&&password.text.length>=6&&if(isSchool)school.text.isNotBlank()&&contact.text.isNotBlank()&&address.text.isNotBlank() else name.text.isNotBlank()&&classInput.text.isNotBlank();if(!ok){status.text="Please complete all required fields. Password must be at least 6 characters.";return@setOnClickListener};progress.visibility=View.VISIBLE;authRepository.register(name=if(isSchool)school.text.toString().trim() else name.text.toString().trim(),email=email.text.toString().trim(),password=password.text.toString(),role=selectedRole,schoolName=if(isSchool)school.text.toString().trim() else "",className=classInput.text.toString().trim(),contactNumber=contact.text.toString().trim(),address=address.text.toString().trim()){success,msg->progress.visibility=View.GONE;status.text=msg;if(success)routeCurrentUser(status,progress)}}
        login.setOnClickListener{if(email.text.isBlank()||password.text.isBlank()){status.text="Enter email and password";return@setOnClickListener};progress.visibility=View.VISIBLE;firebaseAuth.signInWithEmailAndPassword(email.text.toString().trim(),password.text.toString()).addOnSuccessListener{userResult->val uid=userResult.user?.uid?:return@addOnSuccessListener;db.collection("users").document(uid).get().addOnSuccessListener{doc->progress.visibility=View.GONE;val role=doc.getString("role");if(adminLoginMode){if(role=="admin")open(AdminActivity::class.java)else{firebaseAuth.signOut();status.text="Access denied. This account is not an EduNexa administrator."}}else{routeCurrentUser(status,progress)}}.addOnFailureListener{progress.visibility=View.GONE;status.text=it.message?:"Could not verify account role"}}.addOnFailureListener{progress.visibility=View.GONE;status.text=it.message?:"Login failed"}}
    }
    override fun onStart(){super.onStart();if(::status.isInitialized&&firebaseAuth.currentUser!=null&&!routingSession)routeCurrentUser(status,progress)}
    private fun routeCurrentUser(status:TextView,progress:ProgressBar){if(routingSession)return;val uid=firebaseAuth.currentUser?.uid?:run{progress.visibility=View.GONE;return};routingSession=true;progress.visibility=View.VISIBLE;db.collection("users").document(uid).get().addOnSuccessListener{doc->routingSession=false;progress.visibility=View.GONE;when(doc.getString("role")){"admin"->open(AdminActivity::class.java);"school"->if(doc.getBoolean("schoolApproved")==true)open(SchoolActivity::class.java)else open(PendingApprovalActivity::class.java);"student"->open(MainActivity::class.java);else->{status.text="Account role is not configured";firebaseAuth.signOut()}}}.addOnFailureListener{routingSession=false;progress.visibility=View.GONE;status.text=it.message?:"Could not restore account. Check your internet and try again."}}
    private fun open(target:Class<*>){startActivity(Intent(this,target).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish()}
}
