package com.edunexa.app
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
class TargetKbToolActivity:AppCompatActivity(){private lateinit var size:EditText;private lateinit var status:TextView;override fun onCreate(b:Bundle?){super.onCreate(b);val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(36,36,36,36)};r.addView(TextView(this).apply{text="Target KB Compressor";textSize=24f;setTypeface(typeface,1)});size=EditText(this).apply{hint="Target size KB: 10, 20, 50, 100...";inputType=2};status=TextView(this);r.addView(size);r.addView(Button(this).apply{text="Select & Compress Image";setOnClickListener{startActivityForResult(Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI),91)}});r.addView(status);setContentView(r)}
override fun onActivityResult(req:Int,res:Int,data:Intent?){super.onActivityResult(req,res,data);if(req!=91||res!=Activity.RESULT_OK)return;val target=size.text.toString().toIntOrNull();if(target==null||target<1){status.text="Enter target KB first";return};val uri=data?.data?:return;val bmp=contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)}?:return;var q=95;var bytes=ByteArray(0);while(q>=5){val out=ByteArrayOutputStream();bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG,q,out);bytes=out.toByteArray();if(bytes.size<=target*1024)break;q-=5};if(bytes.size>target*1024){status.text="Could not reach ${target}KB without resizing";return};val name="EduNexa_${System.currentTimeMillis()}.jpg";contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,android.content.ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,name);put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")})?.let{u->contentResolver.openOutputStream(u)?.use{it.write(bytes)};status.text="Saved: ${bytes.size/1024}KB • $name"}?:run{status.text="Save failed"}}}
