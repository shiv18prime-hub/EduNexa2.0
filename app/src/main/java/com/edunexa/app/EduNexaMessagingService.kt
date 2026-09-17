package com.edunexa.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class EduNexaMessagingService : FirebaseMessagingService() {
 override fun onNewToken(token:String){super.onNewToken(token);FirebaseAuth.getInstance().currentUser?.uid?.let{uid->FirebaseFirestore.getInstance().collection("users").document(uid).update("fcmToken",token,"fcmTokenUpdatedAt",FieldValue.serverTimestamp())}}
 override fun onMessageReceived(message:RemoteMessage){super.onMessageReceived(message);val title=message.notification?.title?:message.data["title"]?:"EduNexa";val body=message.notification?.body?:message.data["body"]?:"You have a new update";show(title,body)}
 private fun show(title:String,body:String){val manager=getSystemService(NotificationManager::class.java);val channel="edunexa_updates";if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel(channel,"EduNexa Updates",NotificationManager.IMPORTANCE_DEFAULT));val intent=Intent(this,AuthActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP};val pending=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);val notification=NotificationCompat.Builder(this,channel).setSmallIcon(R.drawable.edunexa_logo).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(pending).build();try{manager.notify((System.currentTimeMillis()%100000).toInt(),notification)}catch(_:SecurityException){}}
}
