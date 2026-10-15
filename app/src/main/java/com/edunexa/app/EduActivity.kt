package com.edunexa.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat

open class EduActivity : AppCompatActivity() {
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val content = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
            insets
        }
        val uid=com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if(uid!=null){com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener{token->com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).update("fcmToken",token,"fcmTokenUpdatedAt",com.google.firebase.firestore.FieldValue.serverTimestamp())}}
        if(android.os.Build.VERSION.SDK_INT>=33&&this !is AuthActivity){val prefs=getSharedPreferences("edunexa-notifications",MODE_PRIVATE);if(!prefs.getBoolean("asked",false)&&androidx.core.content.ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){prefs.edit().putBoolean("asked",true).apply();androidx.core.app.ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),1001)}}
        ViewCompat.requestApplyInsets(content)
    }
}
