package com.edunexa.app

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.edunexa.app.data.SchoolJoinRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

class QrActivity : AppCompatActivity() {
 private val db=FirebaseFirestore.getInstance();private val auth=FirebaseAuth.getInstance();private val joinRepo=SchoolJoinRepository();private lateinit var status:TextView;private lateinit var preview:ImageView
 private val imagePicker=registerForActivityResult(ActivityResultContracts.GetContent()){uri->uri?:return@registerForActivityResult;runCatching{val bitmap=if(android.os.Build.VERSION.SDK_INT>=28)android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(contentResolver,uri)){decoder,info,_->decoder.setTargetSampleSize(kotlin.math.max(1,kotlin.math.max(info.size.width,info.size.height)/1600))}else @Suppress("DEPRECATION") android.provider.MediaStore.Images.Media.getBitmap(contentResolver,uri);decode(bitmap)}.onFailure{status.text="Could not read QR from image"}}
 override fun onCreate(b:Bundle?){super.onCreate(b);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,28,28,28)};root.addView(TextView(this).apply{text="EduNexa QR Center";textSize=25f;setTypeface(typeface,1)});status=TextView(this).apply{text="Scan or generate a verified EduNexa QR";setPadding(0,12,0,18)};root.addView(status);preview=ImageView(this).apply{adjustViewBounds=true;minimumHeight=320};root.addView(preview,LinearLayout.LayoutParams(-1,360));root.addView(Button(this).apply{text="Scan QR with Camera";isAllCaps=false;setOnClickListener{openCameraScanner()}});root.addView(Button(this).apply{text="Scan QR from Image";isAllCaps=false;setOnClickListener{imagePicker.launch("image/*")}});root.addView(Button(this).apply{text="Generate My EduNexa QR";isAllCaps=false;setOnClickListener{generateForCurrentUser()}});setContentView(ScrollView(this).apply{addView(root)})}
 private fun openCameraScanner(){val options=GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build();GmsBarcodeScanning.getClient(this,options).startScan().addOnSuccessListener{handlePayload(it.rawValue.orEmpty())}.addOnCanceledListener{status.text="Scan cancelled"}.addOnFailureListener{status.text=it.message?:"Camera scanner unavailable"}}
 private fun decode(bitmap:Bitmap){val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height);val result=MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width,bitmap.height,pixels))));handlePayload(result.text)}
 private fun handlePayload(raw:String){val payload=raw.trim();when{payload.startsWith("EDUNEXA:SCHOOL:")->joinRepo.joinByCode(payload.substringAfterLast(":")){ok,msg->status.text=if(ok)"✓ $msg" else msg};payload.startsWith("EDUNEXA:RESULT:")->startActivity(Intent(this,VerifyMarksheetActivity::class.java).putExtra("verificationId",payload.substringAfterLast(":")));else->status.text="QR scanned successfully\n$payload"}}
 private fun generateForCurrentUser(){val uid=auth.currentUser?.uid?:return;db.collection("users").document(uid).get().addOnSuccessListener{d->val role=d.getString("role")?:"student";val payload=if(role=="school"&&d.getBoolean("schoolApproved")==true)"EDUNEXA:SCHOOL:${d.getString("schoolCode")?:uid}" else "EDUNEXA:STUDENT:$uid";val matrix=QRCodeWriter().encode(payload,BarcodeFormat.QR_CODE,700,700);val bmp=Bitmap.createBitmap(700,700,Bitmap.Config.RGB_565);for(x in 0 until 700)for(y in 0 until 700)bmp.setPixel(x,y,if(matrix[x,y])android.graphics.Color.BLACK else android.graphics.Color.WHITE);preview.setImageBitmap(bmp);status.text="✓ EduNexa QR generated"}.addOnFailureListener{status.text="Could not verify account for QR generation"}}
}
