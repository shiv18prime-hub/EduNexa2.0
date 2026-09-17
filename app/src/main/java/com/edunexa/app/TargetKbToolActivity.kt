package com.edunexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

class TargetKbToolActivity : AppCompatActivity() {
    private lateinit var size: EditText
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private lateinit var save: Button
    private lateinit var qualityText: TextView
    private var source: Bitmap?=null
    private var pendingBytes: ByteArray?=null
    private var maxQuality=95
    private var unsaved=false
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun bg(c:String)=GradientDrawable().apply { setColor(Color.parseColor(c));cornerRadius=dp(16).toFloat() }

    override fun onCreate(b:Bundle?) {
        super.onCreate(b)
        val r=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(20),dp(18),dp(30));setBackgroundColor(Color.parseColor("#F6F7FC")) }
        r.addView(TextView(this).apply { text="Target KB Compressor";textSize=26f;setTypeface(typeface,1);setTextColor(Color.parseColor("#17213D")) })
        r.addView(TextView(this).apply { text="Set target • preview • adjust quality • save";setTextColor(Color.parseColor("#72798F"));setPadding(0,dp(5),0,dp(14)) })
        val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(14));background=bg("#FFFFFF") }
        size=EditText(this).apply { hint="Target size KB: 20, 50, 100...";inputType=2 }
        card.addView(size)
        card.addView(Button(this).apply { text="Choose Image";isAllCaps=false;setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="image/*";addCategory(Intent.CATEGORY_OPENABLE) },91) } })
        r.addView(card)
        r.addView(TextView(this).apply { text="LIVE PREVIEW";setTypeface(typeface,1);setTextColor(Color.parseColor("#3157D5"));setPadding(0,dp(18),0,dp(7)) })
        preview=ImageView(this).apply { adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER;background=bg("#FFFFFF") }
        r.addView(preview,LinearLayout.LayoutParams(-1,dp(390)))
        qualityText=TextView(this).apply { text="Max quality: 95%";setPadding(0,dp(10),0,0) };r.addView(qualityText)
        r.addView(SeekBar(this).apply { max=90;progress=90;setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(s:SeekBar?,p:Int,f:Boolean){maxQuality=p+5;qualityText.text="Max quality: $maxQuality%"};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){if(source!=null)compress()} }) })
        r.addView(Button(this).apply { text="Recompress / Update Preview";isAllCaps=false;setOnClickListener { compress() } })
        save=Button(this).apply { text="Save Compressed Image";isAllCaps=false;visibility=View.GONE;setOnClickListener { chooseSave() } };r.addView(save)
        status=TextView(this).apply { text="Nothing is saved automatically.";setPadding(0,dp(10),0,0) };r.addView(status)
        setContentView(ScrollView(this).apply { addView(r) })
    }

    private fun compress() {
        val original=source?:return
        val targetKb=size.text.toString().toIntOrNull()
        if(targetKb==null || targetKb !in 5..5000) { status.text="Enter target between 5 KB and 5000 KB";return }
        status.text="Compressing preview..."
        runCatching {
            var work=original
            var best=ByteArray(0)
            var rounds=0
            while(rounds<10) {
                var low=10;var high=maxQuality;var found:ByteArray?=null
                while(low<=high) {
                    val q=(low+high)/2
                    val out=ByteArrayOutputStream();work.compress(Bitmap.CompressFormat.JPEG,q,out);val bytes=out.toByteArray()
                    if(bytes.size<=targetKb*1024) { found=bytes;low=q+1 } else high=q-1
                }
                if(found!=null) { best=found;break }
                val test=ByteArrayOutputStream();work.compress(Bitmap.CompressFormat.JPEG,10,test);best=test.toByteArray()
                if(work.width<=180 || work.height<=180) break
                val ratio=sqrt((targetKb*1024.0/best.size).coerceIn(0.35,0.90))*0.96
                val nw=(work.width*ratio).toInt().coerceAtLeast(160);val nh=(work.height*ratio).toInt().coerceAtLeast(160)
                val next=Bitmap.createScaledBitmap(work,nw,nh,true)
                if(work !== original && !work.isRecycled)work.recycle();work=next;rounds++
            }
            pendingBytes=best
            val p=BitmapFactory.decodeByteArray(best,0,best.size);preview.setImageBitmap(p);save.visibility=View.VISIBLE;unsaved=true
            status.text="Preview • ${best.size/1024} KB • ${p.width}×${p.height}px • target $targetKb KB"
            if(work !== original && !work.isRecycled)work.recycle()
        }.onFailure { status.text="Compression failed: ${it.message}";save.visibility=View.GONE }
    }

    private fun decodeSampled(uri:Uri,maxSide:Int=2400):Bitmap? {
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,bounds) }
        if(bounds.outWidth<=0||bounds.outHeight<=0)return null
        var sample=1;while(bounds.outWidth/sample>maxSide*2||bounds.outHeight/sample>maxSide*2)sample*=2
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize=sample;inPreferredConfig=Bitmap.Config.RGB_565 }) }
    }

    override fun onActivityResult(req:Int,res:Int,data:Intent?) {
        super.onActivityResult(req,res,data);if(res!=Activity.RESULT_OK)return
        if(req==91) {
            val next=data?.data?.let { decodeSampled(it) }
            if(next==null){status.text="Could not open this image";return}
            source?.let { if(!it.isRecycled)it.recycle() };source=next;pendingBytes=null;unsaved=false;save.visibility=View.GONE
            if(size.text.isBlank())size.setText("100");compress()
        } else if(req==92) {
            val uri=data?.data?:return;val bytes=pendingBytes?:return
            runCatching { contentResolver.openOutputStream(uri)?.use { it.write(bytes) };unsaved=false;status.text="Saved successfully • ${bytes.size/1024} KB" }.onFailure { status.text="Save failed: ${it.message}" }
        }
    }

    private fun chooseSave(){if(pendingBytes==null)return;startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE);type="image/jpeg";putExtra(Intent.EXTRA_TITLE,"EduNexa_${System.currentTimeMillis()}.jpg") },92)}

    @Suppress("DEPRECATION")
    override fun onBackPressed(){if(unsaved)AlertDialog.Builder(this).setTitle("Discard compressed image?").setMessage("The compressed preview has not been saved.").setPositiveButton("Discard"){_,_->unsaved=false;super.onBackPressed()}.setNegativeButton("Stay",null).show()else super.onBackPressed()}
}
