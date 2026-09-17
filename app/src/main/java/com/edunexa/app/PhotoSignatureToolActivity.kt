package com.edunexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PhotoSignatureToolActivity : AppCompatActivity() {
    private lateinit var w: EditText
    private lateinit var h: EditText
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private lateinit var save: Button
    private var source: Bitmap? = null
    private var output: Bitmap? = null
    private var rotation = 0f
    private var unsaved = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(c: String) = GradientDrawable().apply { setColor(Color.parseColor(c)); cornerRadius = dp(16).toFloat() }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18),dp(20),dp(18),dp(30)); setBackgroundColor(Color.parseColor("#F6F7FC")) }
        root.addView(TextView(this).apply { text="Photo & Signature Maker"; textSize=26f; setTypeface(typeface,1); setTextColor(Color.parseColor("#17213D")) })
        root.addView(TextView(this).apply { text="Resize • rotate • preview • edit • save"; setTextColor(Color.parseColor("#72798F")); setPadding(0,dp(5),0,dp(14)) })
        val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(14),dp(10),dp(14),dp(14)); background=bg("#FFFFFF") }
        w=EditText(this).apply { hint="Width px (example 300)"; inputType=2 }
        h=EditText(this).apply { hint="Height px (example 400)"; inputType=2 }
        card.addView(w); card.addView(h)
        card.addView(Button(this).apply { text="Choose Image"; isAllCaps=false; setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="image/*"; addCategory(Intent.CATEGORY_OPENABLE) },81) } })
        root.addView(card)
        root.addView(TextView(this).apply { text="LIVE PREVIEW"; setTypeface(typeface,1); setTextColor(Color.parseColor("#3157D5")); setPadding(0,dp(18),0,dp(7)) })
        preview=ImageView(this).apply { adjustViewBounds=true; scaleType=ImageView.ScaleType.FIT_CENTER; background=bg("#FFFFFF") }
        root.addView(preview,LinearLayout.LayoutParams(-1,dp(390)))
        val edit=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        edit.addView(Button(this).apply { text="↶ Rotate"; isAllCaps=false; setOnClickListener { rotation-=90; render() } },LinearLayout.LayoutParams(0,-2,1f))
        edit.addView(Button(this).apply { text="Rotate ↷"; isAllCaps=false; setOnClickListener { rotation+=90; render() } },LinearLayout.LayoutParams(0,-2,1f))
        root.addView(edit)
        root.addView(Button(this).apply { text="Apply Width / Height"; isAllCaps=false; setOnClickListener { render() } })
        save=Button(this).apply { text="Save Image"; isAllCaps=false; visibility=View.GONE; setOnClickListener { if(output!=null) startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="image/jpeg"; putExtra(Intent.EXTRA_TITLE,"EduNexa_Form_${System.currentTimeMillis()}.jpg") },82) } }
        root.addView(save)
        status=TextView(this).apply { text="Choose an image. Nothing is saved until you tap Save."; setPadding(0,dp(10),0,0) }
        root.addView(status)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun render() {
        val src=source ?: return
        val width=w.text.toString().toIntOrNull(); val height=h.text.toString().toIntOrNull()
        if(width==null || height==null || width !in 1..5000 || height !in 1..5000 || width.toLong()*height > 20_000_000L) {
            status.text="Use dimensions up to 5000 px and 20 megapixels"; save.visibility=View.GONE; return
        }
        runCatching {
            val scaled=Bitmap.createScaledBitmap(src,width,height,true)
            val m=Matrix().apply { postRotate(rotation) }
            val next=Bitmap.createBitmap(scaled,0,0,scaled.width,scaled.height,m,true)
            if(scaled !== src && scaled !== next && !scaled.isRecycled) scaled.recycle()
            output?.let { if(it !== next && it !== source && !it.isRecycled) it.recycle() }
            output=next; preview.setImageBitmap(next); save.visibility=View.VISIBLE; unsaved=true
            status.text="Editable preview • ${next.width}×${next.height}px • Rotation ${(rotation.toInt()%360+360)%360}°"
        }.onFailure { status.text="Could not process image: ${it.message}"; save.visibility=View.GONE }
    }

    private fun decodeSampled(uri: Uri, maxSide: Int = 2200): Bitmap? {
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,bounds) }
        if(bounds.outWidth<=0 || bounds.outHeight<=0) return null
        var sample=1
        while(bounds.outWidth/sample > maxSide*2 || bounds.outHeight/sample > maxSide*2) sample*=2
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize=sample; inPreferredConfig=Bitmap.Config.ARGB_8888 }) }
    }

    override fun onActivityResult(q:Int,res:Int,data:Intent?) {
        super.onActivityResult(q,res,data); if(res!=Activity.RESULT_OK)return
        if(q==81) {
            val next=data?.data?.let { decodeSampled(it) }
            if(next==null) { status.text="Could not open this image"; return }
            source?.let { if(it !== output && !it.isRecycled) it.recycle() }; source=next
            output=null; unsaved=false; if(w.text.isBlank())w.setText("300"); if(h.text.isBlank())h.setText("400"); rotation=0f; render()
        } else if(q==82) {
            val uri=data?.data?:return; val bmp=output?:return
            runCatching { contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG,92,it) }; unsaved=false; status.text="Saved successfully" }.onFailure { status.text="Save failed: ${it.message}" }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if(unsaved) AlertDialog.Builder(this).setTitle("Discard unsaved image?").setMessage("Your edited image has not been saved.").setPositiveButton("Discard") { _,_->unsaved=false;super.onBackPressed() }.setNegativeButton("Stay",null).show() else super.onBackPressed()
    }
}
