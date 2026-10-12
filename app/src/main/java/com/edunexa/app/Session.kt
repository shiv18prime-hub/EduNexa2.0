package com.edunexa.app
import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
object Session {
 fun logout(activity:Activity){
  var finished=false
  fun close(){
   if(finished)return
   finished=true
   FirebaseAuth.getInstance().signOut()
   activity.startActivity(Intent(activity,AuthActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK})
   activity.finish()
  }
  val uid=FirebaseAuth.getInstance().currentUser?.uid
  if(uid==null){close();return}
  FirebaseFirestore.getInstance().collection("users").document(uid).update("fcmToken","").addOnCompleteListener{close()}
  Handler(Looper.getMainLooper()).postDelayed({close()},1500)
 }
}
