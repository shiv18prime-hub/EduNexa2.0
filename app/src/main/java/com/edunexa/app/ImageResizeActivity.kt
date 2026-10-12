package com.edunexa.app
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.edunexa.app.tools.SafeImages
import java.util.concurrent.Executors
class ImageResizeActivity:EduActivity(){
 private lateinit var status:TextView;private lateinit var preview:ImageView;private lateinit var width:EditText;private lateinit var height:EditText;private var source:Uri?=null;private var output:Bitmap?=null;private val executor=Executors.newSingleThreadExecutor()
 private val picker=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->source=uri;status.text="Set the output dimensions"}
 private val saver=registerForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")){uri->if(uri!=null)runCatching{val b=output?:error("Create a preview first");contentResolver.openOutputStream(uri)?.use{b.compress(Bitmap.CompressFormat.JPEG,92,it)}?:error("Could not open output");status.text="Saved"}.onFailure{status.text=it.message}}
 override fun onCreate(state:Bundle?){super.onCreate(state);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,24,28,28)};setContentView(ScrollView(this).apply{addView(root)});root.addView(TextView(this).apply{text="Image Resizer";textSize=27f});width=EditText(this).apply{hint="Width in pixels";inputType=android.text.InputType.TYPE_CLASS_NUMBER};height=EditText(this).apply{hint="Height in pixels";inputType=android.text.InputType.TYPE_CLASS_NUMBER};root.addView(width);root.addView(height);status=TextView(this);root.addView(status);preview=ImageView(this).apply{adjustViewBounds=true};root.addView(preview,LinearLayout.LayoutParams(-1,400));root.addView(Button(this).apply{text="Choose Image";setOnClickListener{picker.launch(arrayOf("image/*"))}});root.addView(Button(this).apply{text="Resize Preview";setOnClickListener{val uri=source?:return@setOnClickListener;val w=width.text.toString().toIntOrNull()?:0;val h=height.text.toString().toIntOrNull()?:0;if(w !in 1..4096||h !in 1..4096||w.toLong()*h>10000000){status.text="Use dimensions up to 4096 pixels and 10 megapixels";return@setOnClickListener};isEnabled=false;executor.execute{runCatching{val src=SafeImages.decode(this@ImageResizeActivity,uri);val resized=Bitmap.createScaledBitmap(src,w,h,true);if(src!==resized)src.recycle();resized}.onSuccess{b->runOnUiThread{if(!isDestroyed){output=b;preview.setImageBitmap(b);status.text="$w × $h pixels";isEnabled=true}else b.recycle()}}.onFailure{e->runOnUiThread{isEnabled=true;status.text=e.message}}}}});root.addView(Button(this).apply{text="Save JPG";setOnClickListener{if(output!=null)saver.launch("EduNexa_Resized.jpg")}})}
 override fun onDestroy(){executor.shutdownNow();super.onDestroy()}
}
