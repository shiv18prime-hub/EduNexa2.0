package com.edunexa.app
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
class PdfToolkitActivity:AppCompatActivity(){private lateinit var status:TextView;private lateinit var preview:ImageView;private lateinit var save:Button;private var bitmap:Bitmap?=null;private var pendingPdf:PdfDocument?=null;override fun onCreate(b:Bundle?){super.onCreate(b);val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(36,36,36,36)};r.addView(TextView(this).apply{text="PDF Toolkit";textSize=26f;setTypeface(typeface,1)});r.addView(TextView(this).apply{text="Select → Preview → Save\nNothing is saved until you tap Save.";setPadding(0,10,0,22)});r.addView(Button(this).apply{text="Choose Image for PDF";isAllCaps=false;setOnClickListener{startActivityForResult(Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI),71)}});preview=ImageView(this).apply{adjustViewBounds=true;visibility=android.view.View.GONE};r.addView(preview,LinearLayout.LayoutParams(-1,520));save=Button(this).apply{text="Save PDF";isAllCaps=false;isEnabled=false;setOnClickListener{prepareAndChooseSave()}};r.addView(save);arrayOf("PDF → Images","Merge PDFs","Split PDF","Compress PDF","Reorder / Delete Pages").forEach{n->r.addView(Button(this).apply{text=n;isAllCaps=false;setOnClickListener{Toast.makeText(this@PdfToolkitActivity,"$n is being upgraded",Toast.LENGTH_SHORT).show()}})};status=TextView(this).apply{gravity=Gravity.CENTER;setPadding(0,20,0,10)};r.addView(status);setContentView(ScrollView(this).apply{addView(r)})}
override fun onActivityResult(q:Int,res:Int,data:Intent?){super.onActivityResult(q,res,data);if(q==71&&res==Activity.RESULT_OK){val uri=data?.data?:return;bitmap=contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)};bitmap?.let{preview.setImageBitmap(it);preview.visibility=android.view.View.VISIBLE;save.isEnabled=true;status.text="Preview ready. Tap Save PDF when you are happy."}}else if(q==72&&res==Activity.RESULT_OK){val uri=data?.data?:return;val pdf=pendingPdf?:return;runCatching{contentResolver.openOutputStream(uri)?.use{pdf.writeTo(it)};status.text="PDF saved successfully"}.onFailure{status.text="Could not save PDF: ${it.message}"};pdf.close();pendingPdf=null}}
private fun prepareAndChooseSave(){val bmp=bitmap?:return;pendingPdf?.close();val pdf=PdfDocument();val page=pdf.startPage(PdfDocument.PageInfo.Builder(bmp.width,bmp.height,1).create());page.canvas.drawBitmap(bmp,0f,0f,null);pdf.finishPage(page);pendingPdf=pdf;startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/pdf";putExtra(Intent.EXTRA_TITLE,"EduNexa_${System.currentTimeMillis()}.pdf")},72)}
override fun onDestroy(){pendingPdf?.close();pendingPdf=null;super.onDestroy()}}
