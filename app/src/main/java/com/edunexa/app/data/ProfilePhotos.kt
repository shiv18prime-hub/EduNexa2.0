package com.edunexa.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.Executors

/** Small profile images live with the user's Firestore profile, so uploads work on Spark. */
object ProfilePhotos {
 private val executor=Executors.newFixedThreadPool(2)
 fun upload(context:Context,uri:Uri,onSuccess:(String)->Unit,onError:(String)->Unit){
  val uid=FirebaseAuth.getInstance().currentUser?.uid?:return onError("Please sign in")
  executor.execute{try{
   val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
   require(bounds.outWidth>0&&bounds.outHeight>0){"Choose a valid image"};var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>512)sample*=2
   val bitmap=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample})}?:error("Could not read photo")
   val out=ByteArrayOutputStream();var quality=85
   while(quality>=45){out.reset();bitmap.compress(Bitmap.CompressFormat.JPEG,quality,out);if(out.size()<=180*1024)break;quality-=10}
   bitmap.recycle();require(out.size()<=180*1024){"Choose a smaller photo"}
   val data="data:image/jpeg;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP)
   FirebaseFirestore.getInstance().collection("users").document(uid).update("profilePhotoData",data).addOnSuccessListener{onSuccess(data)}.addOnFailureListener{onError(it.message?:"Could not save profile photo")}
  }catch(e:Exception){android.os.Handler(android.os.Looper.getMainLo