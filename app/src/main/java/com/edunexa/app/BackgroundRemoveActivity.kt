package com.edunexa.app
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.edunexa.app.data.Backend
import com.edunexa.app.tools.SafeImages
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.util.concurrent.Executors
class BackgroundRemoveActivity:EduActivity(){
 private lateinit var status:TextView;private lateinit var preview:ImageView;private var output:Bitmap?=null;private var allowed=false;private val executor=Executors.newSingleThreadExecutor();private val segmenter=Segmentation.getClient(SelfieSegmenterOptions.Builder().setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE).build())
 private val picker=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null&&allowed){status.text="Processing portrait…";executor.execute{runCatching{SafeImages.decode(this,uri,1200)}.onSuccess{b->segmenter.process(InputImage.fromBitmap(b,0)).addOnSuccessListener{mask->executor.execute{val pixels=IntArray(b.width*b.height);b.getPixels(pixels,0,b.width,0,0,b.width,b.height);val buffer=mask.buffer;buffer.rewind();for(i in pixels.indices){val alpha=(buffer.float.coerceIn(0f,1f)*255).toInt();pixels[i]=(pixels[i] and 0x00FFFFFF) or (alpha shl 24)};val result=Bitmap.createBitmap(pixels,b.width,b.height,Bitmap.Config.ARGB_8888);b.recycle();runOnUiThread{if(!isDestroyed){output=result;preview.setImageBitmap(result);status.text="Preview ready • transparent PNG"}else result.recycle()}}}.addOnFailureListener{e->b.recycle();status.text=e.message}}.onFailure{e->runOnUiThread{status.text=e.message}}}}}
 private val saver=registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")){uri->if(uri!=null)runCatching{contentResolver.openOutputStream(uri)?.use{output?.compress(Bitmap.CompressFormat.PNG,100,it)}?:error("Could not save");status.text="PNG saved"}.onFailure{status.text=it.message}}
 override fun onCreate(state:Bundle?){super.onCreate(state);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,24,28,28)};setContentView(ScrollView(this).apply{addView(root)});root.addView(TextView(this).apply{text="Portrait Background Remover";textSize=25f});root.addView(TextView(this).apply{text="For photos of people. Check the hair and edges in the preview before saving."});status=TextView(this);root.addView(status);preview=ImageView(this).apply{adjustViewBounds=true;setBackgroundColor(android.graphics.Color.LTGRAY)};root.addView(preview,LinearLayout.LayoutParams(-1,500));root.addView(Button(this).apply{text="Choose Portrait";setOnClickListener{if(allowed)picker.launch(arrayOf("image/*"))else startActivity(Intent(this@BackgroundRemoveActivity,MembershipActivity::class.java))}});root.addView(Button(this).apply{text="Save Transparent PNG";setOnClickListener{if(output!=null)saver.launch("EduNexa_Portrait.png")}})}
 override fun onResume(){super.onResume();Backend.call("dashboard",success={r->allowed=r["pro"]==true;status.text=if(allowed)"Pro active • choose a portrait" else "Pro membership required"},failure={status.text=it})}
 override fun onDestroy(){segmenter.close();executor.shutdownNow();super.onDestroy()}
}
