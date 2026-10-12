package com.edunexa.app.data
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.Executors

object ProfilePhotos {
 private val executor=Executors.newFixedThreadPool(2)
 fun upload(context:Context,uri:Uri,onSuccess:(String)->Unit,onError:(String)->Unit){val uid=FirebaseAuth.getInstance().currentUser?.uid?:return onError("Please sign in");executor.execute{try{val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)};require(bounds.outWidth>0&&bounds.outHeight>0){"Choose a valid image"};var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>512)sample*=2;val bitmap=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample})}?:error("Could not read photo");val out=ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);bitmap.recycle();val bytes=out.toByteArray();require(bytes.size<1024*1024){"Choose a smaller photo"};val metadata=com.google.firebase.storage.StorageMetadata.Builder().setContentType("image/jpeg").build();val ref=FirebaseStorage.getInstance().reference.child("profilePhotos/$uid/avatar.jpg");ref.putBytes(bytes,metadata).continueWithTask{task->if(!task.isSuccessful)throw task.exception?:IllegalStateException("Upload failed");ref.downloadUrl}.addOnSuccessListener{url->FirebaseFirestore.getInstance().collection("users").document(uid).update("profilePhotoUrl",url.toString()).addOnSuccessListener{onSuccess(url.toString())}.addOnFailureListener{onError(it.message?:"Could not update profile")}}.addOnFailureListener{onError(it.message?:"Could not upload photo")}}catch(e:Exception){android.os.Handler(android.os.Looper.getMainLooper()).post{onError(e.message?:"Could not read photo")}}}}
 fun load(view:ImageView,url:String){if(!url.startsWith("https://firebasestorage.googleapis.com/"))return;view.tag=url;executor.execute{try{val conn=URL(url).openConnection().apply{connectTimeout=10000;readTimeout=10000};val bytes=conn.getInputStream().use{it.readBytes()};require(bytes.size<1024*1024);val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size);view.post{if(view.tag==url)view.setImageBitmap(bitmap)}}catch(_:Exception){}}}
}
