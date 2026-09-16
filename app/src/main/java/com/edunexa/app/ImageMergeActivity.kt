package com.edunexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.max

class ImageMergeActivity : AppCompatActivity() {
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var save: Button
    private var resultBytes: ByteArray? = null
    private var unsaved = false

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(36,36,36,36)}
        root.addView(TextView(this).apply{text="Image Merge";textSize=26f;setTypeface(typeface,1)})
        root.addView(TextView(this).apply{text="Select multiple photos → Preview → Save\nNothing is saved automatically.";setPadding(0,8,0,20)})
        root.addView(Button(this).apply{text="Select Images";isAllCaps=false;setOnClickListener{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)},301)}})
        preview=ImageView(this).apply{adjustViewBounds=true;visibility=View.GONE};root.addView(preview,LinearLayout.LayoutParams(-1,650))
        save=Button(this).apply{text="Save Merged Image";isAllCaps=false;visibility=View.GONE;setOnClickListener{chooseSave()}};root.addView(save)
        status=TextView(this).apply{setPadding(0,16,0,0)};root.addView(status)
        setContentView(ScrollView(this).apply{addView(root)})
    }

    override fun onActivityResult(req:Int,res:Int,data:Intent?){
        super.onActivityResult(req,res,data);if(res!=Activity.RESULT_OK)return
        if(req==301){
            val uris=mutableListOf<Uri>();data?.clipData?.let{c->for(i in 0 until c.itemCount)uris.add(c.getItemAt(i).uri)};data?.data?.let{uris.add(it)}
            if(uris.size<2){status.text="Please select at least 2 images";return}
            val bitmaps=uris.mapNotNull{u->contentResolver.openInputStream(u)?.use{BitmapFactory.decodeStream(it)}}
            if(bitmaps.size<2){status.text="Could not read selected images";return}
            val merged=mergeGrid(bitmaps);val out=ByteArrayOutputStream();merged.compress(Bitmap.CompressFormat.JPEG,92,out);resultBytes=out.toByteArray();preview.setImageBitmap(merged);preview.visibility=View.VISIBLE;save.visibility=View.VISIBLE;unsaved=true;status.text="${bitmaps.size} images merged • Preview ready • Tap Save to keep it."
        } else if(req==302){
            val uri=data?.data?:return;val bytes=resultBytes?:return
            runCatching{contentResolver.openOutputStream(uri)?.use{it.write(bytes)};unsaved=false;status.text="Merged image saved successfully"}.onFailure{status.text="Save failed: ${it.message}"}
        }
    }

    private fun mergeGrid(src:List<Bitmap>):Bitmap{
        val columns=if(src.size<=2)src.size else 2;val rows=ceil(src.size.toDouble()/columns).toInt();val cellW=720;val cellH=720
        val output=Bitmap.createBitmap(cellW*columns,cellH*rows,Bitmap.Config.ARGB_8888);val canvas=Canvas(output);canvas.drawColor(Color.WHITE)
        src.forEachIndexed{i,b->val scale=max(cellW.toFloat()/b.width,cellH.toFloat()/b.height);val w=(b.width*scale).toInt();val h=(b.height*scale).toInt();val scaled=Bitmap.createScaledBitmap(b,w,h,true);val left=(i%columns)*cellW+(cellW-w)/2f;val top=(i/columns)*cellH+(cellH-h)/2f;canvas.save();canvas.clipRect((i%columns)*cellW,(i/columns)*cellH,(i%columns+1)*cellW,(i/columns+1)*cellH);canvas.drawBitmap(scaled,left,top,null);canvas.restore();if(scaled!==b)scaled.recycle()}
        return output
    }

    private fun chooseSave(){if(resultBytes==null)return;startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="image/jpeg";putExtra(Intent.EXTRA_TITLE,"EduNexa_Merge_${System.currentTimeMillis()}.jpg")},302)}
    override fun onBackPressed(){if(unsaved)AlertDialog.Builder(this).setTitle("Discard merged image?").setMessage("This preview has not been saved.").setPositiveButton("Discard"){_,_->unsaved=false;resultBytes=null;super.onBackPressed()}.setNegativeButton("Stay",null).show() else super.onBackPressed()}
}
