package com.edunexa.app

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class QrCenterActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance(); private val auth = FirebaseAuth.getInstance()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(20),dp(24),dp(20),dp(24));setBackgroundColor(Color.parseColor("#F6F7FC"))}
        root.addView(TextView(this).apply{text="EduNexa QR Center";textSize=27f;setTypeface(typeface,1);setTextColor(Color.parseColor("#20243A"))})
        val status=TextView(this).apply{text="Loading account…";gravity=Gravity.CENTER;setPadding(0,dp(8),0,dp(12));setTextColor(Color.parseColor("#626A80"))};root.addView(status)
        val image=ImageView(this).apply{adjustViewBounds=true};root.addView(image,LinearLayout.LayoutParams(dp(270),dp(270)))
        val generate=Button(this).apply{text="Generate My Verified QR";isAllCaps=false};root.addView(generate,LinearLayout.LayoutParams(-1,dp(56)).apply{setMargins(0,dp(14),0,0)})
        val scan=Button(this).apply{text="▣  Scan EduNexa QR";isAllCaps=false};root.addView(scan,LinearLayout.LayoutParams(-1,dp(56)).apply{setMargins(0,dp(10),0,0)})
        setContentView(ScrollView(this).apply{addView(root)})
        generate.setOnClickListener{generateQr(image,status)}
        val options=GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build();val scanner=GmsBarcodeScanning.getClient(this,options)
        scan.setOnClickListener{scanner.startScan().addOnSuccessListener{barcode->handleScan(barcode.rawValue.orEmpty(),status)}.addOnCanceledListener{status.text="Scan cancelled"}.addOnFailureListener{e->status.text=e.message?:"Scanner unavailable"}}
    }
    private fun generateQr(image:ImageView,status:TextView){
        val uid=auth.currentUser?.uid?:return
        db.collection("users").document(uid).get().addOnSuccessListener{d->
            when(d.getString("role")){
                "school"->{if(d.getBoolean("schoolApproved")!=true){status.text="Admin approval required before school QR can be generated";return@addOnSuccessListener};val code=(d.getString("schoolCode")?:"EDU-${uid.take(8).uppercase()}");val data=hashMapOf<String,Any>("schoolId" to uid,"active" to true,"updatedAt" to FieldValue.serverTimestamp());db.collection("schoolCodes").document(code).set(data).addOnSuccessListener{db.collection("users").document(uid).update("schoolCode",code);showQr("EDUNEXA:SCHOOL:$code",image);status.text="Verified School QR • $code\nStudents can scan this to send a connection request."}.addOnFailureListener{status.text=it.message?:"Could not activate school QR"}}
                "student"->{showQr("EDUNEXA:STUDENT:$uid",image);status.text="Student identification QR\nUse only with trusted EduNexa school/admin screens."}
                else->status.text="QR generation is available for Student and School accounts"
            }
        }.addOnFailureListener{status.text=it.message?:"Could not load account"}
    }
    private fun showQr(value:String,image:ImageView){val size=720;val matrix=QRCodeWriter().encode(value,BarcodeFormat.QR_CODE,size,size);val bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.RGB_565);for(y in 0 until size)for(x in 0 until size)bitmap.setPixel(x,y,if(matrix[x,y])Color.BLACK else Color.WHITE);image.setImageBitmap(bitmap)}
    private fun handleScan(raw:String,status:TextView){when{raw.startsWith("EDUNEXA:SCHOOL:",true)->{val code=raw.substringAfterLast(":");status.text="Verified-format School QR detected: $code\nOpen Connect School to send a request."};raw.startsWith("EDUNEXA:STUDENT:",true)->{val uid=raw.substringAfterLast(":");db.collection("users").document(uid).get().addOnSuccessListener{d->status.text=if(d.getString("role")=="student")"EduNexa Student\n${d.getString("name")?:"Student"} • Class ${d.getString("className")?:"—"}" else "Student record not verified"}.addOnFailureListener{status.text="Could not verify student"}};raw.startsWith("EDUNEXA:RESULT:",true)->verifyResult(raw.substringAfterLast(":"),status);else->status.text="Not a supported EduNexa QR"}}
    private fun verifyResult(id:String,status:TextView){db.collection("marksheetVerifications").document(id).get().addOnSuccessListener{d->status.text=if(d.exists())"✓ EduNexa Verified Result\n${d.getString("studentName")?:"Student"}\nVerification ID: $id" else "Result verification record not found"}.addOnFailureListener{status.text="Could not verify result. Check internet."}}
}
