package com.edunexa.app
import android.app.Activity
import android.content.*
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
class PdfToolkitActivity:AppCompatActivity(){private lateinit var status:TextView;override fun onCreate(b:Bundle?){super.onCreate(b);val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(36,36,36,36)};r.addView(TextView(this).apply{text="PDF Toolkit";textSize=24f;setTypeface(typeface,1)});r.addView(TextView(this).apply{text="Working now: Image → PDF\nMore PDF operations remain visible for the next processing pass."});r.addView(Button(this).apply{text="Image → PDF";setOnClickListener{startActivityForResult(Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI),71)}});arrayOf("PDF → Images","Merge PDFs","Split PDF","Compress PDF","Reorder / Delete Pages").forEach{n->r.addView(Button(this).apply{text=n;setOnClickListener{Toast.makeText(this@PdfToolkitActivity,"$n engine not completed yet",Toast.LENGTH_SHORT).show()}})};status=TextView(this);r.addView(status);setContentView(ScrollView(this).apply{addView(r)})}
override fun onActivityResult(q:Int,res:Int,data:Intent?){super.onActivityResult(q,res,data);if(q!=71||res!=Activity.RESULT_OK)return;val uri=data?.data?:return;val bmp=contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)}?:return;val pdf=PdfDocument();val page=pdf.startPage(PdfDocument.PageInfo.Builder(bmp.width,bmp.height,1).create());page.canvas.drawBitmap(bmp,0f,0f,null);pdf.finishPage(page);val name="EduNexa_${System.currentTimeMillis()}.pdf";val values=ContentValues().apply{put(MediaStore.Downloads.DISPLAY_NAME,name);put(MediaStore.Downloads.MIME_TYPE,"application/pdf")};contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values)?.let{u->contentResolver.openOutputStream(u)?.use{pdf.writeTo(it)};pdf.close();status.text="PDF created and saved: $name"}?:run{pdf.close();status.text="Could not save PDF"}}}
