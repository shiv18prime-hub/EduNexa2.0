package com.edunexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.UUID

class MarksheetMakerActivity : AppCompatActivity() {
    private lateinit var school: EditText; private lateinit var address: EditText; private lateinit var student: EditText
    private lateinit var roll: EditText; private lateinit var clazz: EditText; private lateinit var exam: EditText
    private lateinit var session: EditText; private lateinit var subjects: EditText; private lateinit var preview: ImageView
    private lateinit var status: TextView; private lateinit var save: Button
    private var logo: Bitmap?=null; private var sheet: Bitmap?=null; private var unsaved=false; private var verificationId:String?=null
    private val db=FirebaseFirestore.getInstance()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun rounded(color:String,r:Int,stroke:String?=null)=GradientDrawable().apply{setColor(Color.parseColor(color));cornerRadius=dp(r).toFloat();if(stroke!=null)setStroke(dp(1),Color.parseColor(stroke))}

    override fun onCreate(b:Bundle?){super.onCreate(b)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(20),dp(18),dp(32));setBackgroundColor(Color.parseColor("#F6F7FC"))}
        root.addView(TextView(this).apply{text="Professional Marksheet Maker";textSize=26f;setTextColor(Color.parseColor("#17213D"));setTypeface(typeface,Typeface.BOLD)})
        root.addView(TextView(this).apply{text="Fill details and watch your marksheet update live";textSize=14f;setTextColor(Color.parseColor("#72798F"));setPadding(0,dp(5),0,dp(16))})
        val form=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(15),dp(10),dp(15),dp(15));background=rounded("#FFFFFF",18,"#E4E8F2");elevation=dp(2).toFloat()}
        form.addView(TextView(this).apply{text="Student & School Details";textSize=18f;setTextColor(Color.parseColor("#17213D"));setTypeface(typeface,Typeface.BOLD);setPadding(0,dp(4),0,dp(6))})
        fun field(h:String)=EditText(this).apply{hint=h;textSize=15f;setTextColor(Color.parseColor("#17213D"));setHintTextColor(Color.parseColor("#8A91A5"));setPadding(dp(12),dp(10),dp(12),dp(10));background=rounded("#F8F9FD",12,"#E3E6EF");form.addView(this,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})}
        school=field("School Name");address=field("School Address / Contact / Code");student=field("Student Name");roll=field("Roll / Admission No.");clazz=field("Class / Section");exam=field("Exam Name");session=field("Academic Session");subjects=field("Subjects e.g. Hindi:100:75, English:100:82")
        val logoBtn=Button(this).apply{text="Choose School Logo";isAllCaps=false;setOnClickListener{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE)},401)}}
        form.addView(logoBtn,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)});root.addView(form)
        root.addView(TextView(this).apply{text="LIVE PREVIEW";textSize=13f;setTextColor(Color.parseColor("#3157D5"));setTypeface(typeface,Typeface.BOLD);setPadding(dp(3),dp(20),0,dp(8))})
        val previewCard=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(10),dp(10),dp(10),dp(10));background=rounded("#FFFFFF",18,"#E2E6F0");elevation=dp(3).toFloat()}
        preview=ImageView(this).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER}
        previewCard.addView(preview,LinearLayout.LayoutParams(-1,dp(430)));root.addView(previewCard)
        val generate=Button(this).apply{text="Register Verification & Finalize";isAllCaps=false;setOnClickListener{generate()}}
        root.addView(generate,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(14)})
        save=Button(this).apply{text="Save Marksheet PDF";isAllCaps=false;visibility=View.GONE;setOnClickListener{startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type="application/pdf";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_TITLE,"EduNexa_Marksheet_${verificationId?:System.currentTimeMillis()}.pdf")},402)}}
        root.addView(save,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)});status=TextView(this).apply{text="Preview updates automatically. Nothing is registered or saved until you finalize.";textSize=12f;setTextColor(Color.parseColor("#667085"));setPadding(dp(4),dp(10),dp(4),dp(10))};root.addView(status)
        setContentView(ScrollView(this).apply{setBackgroundColor(Color.parseColor("#F6F7FC"));addView(root)})
        val watcher=object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){updateLivePreview()};override fun afterTextChanged(e:Editable?){}}
        listOf(school,address,student,roll,clazz,exam,session,subjects).forEach{it.addTextChangedListener(watcher)};updateLivePreview()
    }

    private fun updateLivePreview(){val rows=parseSubjects();val max=rows.sumOf{it.second};val got=rows.sumOf{it.third};val pct=if(max>0)got*100.0/max else 0.0;val result=if(rows.isNotEmpty()&&rows.all{it.third*100/it.second>=33})"PASS" else if(rows.isNotEmpty())"FAIL" else "—";preview.setImageBitmap(drawPreview(rows,max,got,pct,result))}
    private fun drawPreview(rows:List<Triple<String,Int,Int>>,maxTotal:Int,gotTotal:Int,pct:Double,result:String):Bitmap{val w=900;val h=1180;val b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);val c=Canvas(b);c.drawColor(Color.WHITE);val p=Paint(1).apply{color=Color.rgb(28,48,94)};c.drawRect(0f,0f,w.toFloat(),14f,p);p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=38f;c.drawText(school.text.toString().ifBlank{"SCHOOL NAME"},w/2f,70f,p);p.typeface=Typeface.DEFAULT;p.textSize=17f;c.drawText(address.text.toString().ifBlank{"School address / contact"},w/2f,100f,p);logo?.let{c.drawBitmap(Bitmap.createScaledBitmap(it,78,78,true),35f,28f,null)};p.typeface=Typeface.DEFAULT_BOLD;p.textSize=26f;c.drawText("ACADEMIC MARKSHEET",w/2f,150f,p);p.textAlign=Paint.Align.LEFT;p.typeface=Typeface.DEFAULT;p.textSize=18f;c.drawText("Student: ${student.text.toString().ifBlank{"—"}}",45f,205f,p);c.drawText("Roll: ${roll.text.toString().ifBlank{"—"}}",490f,205f,p);c.drawText("Class: ${clazz.text.toString().ifBlank{"—"}}",45f,240f,p);c.drawText("Exam: ${exam.text.toString().ifBlank{"—"}}",490f,240f,p);c.drawText("Session: ${session.text.toString().ifBlank{"—"}}",45f,275f,p);var y=335f;p.typeface=Typeface.DEFAULT_BOLD;c.drawText("SUBJECT",45f,y,p);c.drawText("MAX",500f,y,p);c.drawText("MARKS",635f,y,p);c.drawText("GRADE",765f,y,p);p.typeface=Typeface.DEFAULT;if(rows.isEmpty()){y+=48;c.drawText("Add subjects above to preview marks",45f,y,p)}else rows.take(10).forEach{(n,m,g)->y+=48;c.drawText(n.take(30),45f,y,p);c.drawText(m.toString(),500f,y,p);c.drawText(g.toString(),635f,y,p);c.drawText(grade(g,m),765f,y,p)};y+=70;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=20f;c.drawText("Total: $gotTotal / $maxTotal",45f,y,p);c.drawText("Percentage: ${if(maxTotal>0)"%.2f%%".format(pct) else "—"}",330f,y,p);c.drawText("Result: $result",665f,y,p);p.typeface=Typeface.DEFAULT;p.textSize=15f;c.drawText("Live preview • Verification ID will appear after finalizing",45f,h-70f,p);return b}

    override fun onActivityResult(q:Int,res:Int,data:Intent?){super.onActivityResult(q,res,data);if(res!=Activity.RESULT_OK)return;when(q){401->{logo=data?.data?.let{u->contentResolver.openInputStream(u)?.use{BitmapFactory.decodeStream(it)}};updateLivePreview();status.text="Logo selected • Live preview updated"};402->{val uri=data?.data?:return;val bmp=sheet?:return;val pdf=PdfDocument();val page=pdf.startPage(PdfDocument.PageInfo.Builder(bmp.width,bmp.height,1).create());page.canvas.drawBitmap(bmp,0f,0f,null);pdf.finishPage(page);runCatching{contentResolver.openOutputStream(uri)?.use{pdf.writeTo(it)};unsaved=false;status.text="Marksheet PDF saved • Verification ID: $verificationId"}.onFailure{status.text="Save failed: ${it.message}"};pdf.close()}}}
    private fun generate(){val uid=FirebaseAuth.getInstance().currentUser?.uid?:run{status.text="Login required";return};if(school.text.isBlank()||student.text.isBlank()||subjects.text.isBlank()){status.text="Enter school, student and subject marks";return};val rows=parseSubjects();if(rows.isEmpty()){status.text="Use format: Math:100:80, Hindi:100:70";return};status.text="Registering verification...";db.collection("users").document(uid).get().addOnSuccessListener{u->val approved=u.getString("role")=="school"&&u.getBoolean("schoolApproved")==true;val id="EDX-"+UUID.randomUUID().toString().take(8).uppercase();verificationId=id;val max=rows.sumOf{it.second};val got=rows.sumOf{it.third};val pct=got*100.0/max;val result=if(rows.all{it.third*100/it.second>=33})"PASS" else "FAIL";val record=hashMapOf<String,Any>("verificationId" to id,"issuerUid" to uid,"issuerApproved" to approved,"schoolName" to school.text.toString(),"studentName" to student.text.toString(),"rollNo" to roll.text.toString(),"className" to clazz.text.toString(),"examName" to exam.text.toString(),"session" to session.text.toString(),"totalObtained" to got,"totalMax" to max,"percentage" to pct,"result" to result,"createdAt" to FieldValue.serverTimestamp());db.collection("marksheetVerifications").document(id).set(record).addOnSuccessListener{drawFinal(rows,id,max,got,pct,result,approved)}.addOnFailureListener{status.text="Could not register verification: ${it.message}"}}.addOnFailureListener{status.text="Could not check school account: ${it.message}"}}
    private fun drawFinal(rows:List<Triple<String,Int,Int>>,id:String,max:Int,got:Int,pct:Double,result:String,approved:Boolean){val b=drawPreview(rows,max,got,pct,result);val final=Bitmap.createBitmap(1240,1754,Bitmap.Config.ARGB_8888);val c=Canvas(final);c.drawColor(Color.WHITE);c.drawBitmap(Bitmap.createScaledBitmap(b,1160,1520,true),40f,20f,null);val qr=makeQr("EDUNEXA_VERIFY:$id",170);c.drawBitmap(qr,55f,1550f,null);val p=Paint(1).apply{color=Color.rgb(28,48,94);textSize=22f};c.drawText("Verification ID: $id",250f,1605f,p);c.drawText(if(approved)"EDUNEXA VERIFIED SCHOOL RECORD" else "UNOFFICIAL / PRACTICE MARKSHEET",250f,1645f,p);c.drawText("Class Teacher              School Stamp              Principal",250f,1700f,p);sheet=final;preview.setImageBitmap(final);save.visibility=View.VISIBLE;unsaved=true;status.text="Registered • ${if(approved)"Verified school record" else "Unofficial/practice record"} • ID: $id • Tap Save PDF when ready."}
    private fun makeQr(t:String,s:Int):Bitmap{val m=QRCodeWriter().encode(t,BarcodeFormat.QR_CODE,s,s);return Bitmap.createBitmap(s,s,Bitmap.Config.RGB_565).apply{for(x in 0 until s)for(y in 0 until s)setPixel(x,y,if(m[x,y])Color.BLACK else Color.WHITE)}}
    private fun parseSubjects():List<Triple<String,Int,Int>> = subjects.text.toString().split(',').mapNotNull{val p=it.trim().split(':');if(p.size!=3)return@mapNotNull null;val m=p[1].toIntOrNull();val g=p[2].toIntOrNull();if(m!=null&&g!=null&&m>0&&g in 0..m)Triple(p[0].trim(),m,g)else null}
    private fun grade(g:Int,m:Int)=when(g*100/m){in 90..100->"A+";in 80..89->"A";in 70..79->"B+";in 60..69->"B";in 50..59->"C";in 33..49->"D";else->"F"}
    @Deprecated("Deprecated in Java") override fun onBackPressed(){if(unsaved)AlertDialog.Builder(this).setTitle("Discard unsaved PDF?").setMessage("Verification is registered, but PDF is not saved on this device.").setPositiveButton("Discard PDF"){_,_->unsaved=false;super.onBackPressed()}.setNegativeButton("Stay",null).show() else super.onBackPressed()}
}
