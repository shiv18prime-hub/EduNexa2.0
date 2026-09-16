package com.edunexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

class TargetKbToolActivity : AppCompatActivity() {
    private lateinit var size: EditText
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private lateinit var save: Button
    private var pendingBytes: ByteArray? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val r = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36,36,36,36) }
        r.addView(TextView(this).apply { text="Target KB Compressor"; textSize=26f; setTypeface(typeface,1) })
        r.addView(TextView(this).apply { text="Compress → Preview → Save\nNothing is saved automatically."; setPadding(0,8,0,18) })
        size = EditText(this).apply { hint="Target size KB: 10, 20, 50, 100..."; inputType=2 }
        r.addView(size)
        r.addView(Button(this).apply { text="Choose & Compress Image"; isAllCaps=false; setOnClickListener { startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),91) } })
        preview = ImageView(this).apply { adjustViewBounds=true; visibility=View.GONE }
        r.addView(preview, LinearLayout.LayoutParams(-1,520))
        save = Button(this).apply { text="Save Compressed Image"; isAllCaps=false; visibility=View.GONE; setOnClickListener { chooseSave() } }
        r.addView(save)
        status = TextView(this).apply { setPadding(0,16,0,0) }
        r.addView(status)
        setContentView(ScrollView(this).apply { addView(r) })
    }

    override fun onActivityResult(req:Int,res:Int,data:Intent?) {
        super.onActivityResult(req,res,data)
        if(res != Activity.RESULT_OK) return
        if(req == 91) {
            val target=size.text.toString().toIntOrNull()
            if(target==null || target<1){ status.text="Enter target KB first"; return }
            val uri=data?.data?:return
            val bmp=contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)}?:return
            var q=95
            var bytes=ByteArray(0)
            while(q>=5){ val out=ByteArrayOutputStream(); bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG,q,out); bytes=out.toByteArray(); if(bytes.size<=target*1024) break; q-=5 }
            if(bytes.size>target*1024){ pendingBytes=null; save.visibility=View.GONE; status.text="Could not reach ${target}KB without resizing"; return }
            pendingBytes=bytes
            preview.setImageBitmap(BitmapFactory.decodeByteArray(bytes,0,bytes.size))
            preview.visibility=View.VISIBLE
            save.visibility=View.VISIBLE
            status.text="Preview ready • ${bytes.size/1024}KB. Tap Save when ready."
        } else if(req == 92) {
            val uri=data?.data?:return
            val bytes=pendingBytes?:return
            runCatching { contentResolver.openOutputStream(uri)?.use{it.write(bytes)}; status.text="Saved successfully • ${bytes.size/1024}KB" }
                .onFailure { status.text="Save failed: ${it.message}" }
        }
    }

    private fun chooseSave(){
        if(pendingBytes==null) return
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="image/jpeg"; putExtra(Intent.EXTRA_TITLE,"EduNexa_${System.currentTimeMillis()}.jpg") },92)
    }
}
