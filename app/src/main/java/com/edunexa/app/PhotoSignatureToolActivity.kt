package com.edunexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PhotoSignatureToolActivity : AppCompatActivity() {
    private lateinit var w: EditText
    private lateinit var h: EditText
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private lateinit var save: Button
    private var output: Bitmap? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 36)
        }
        root.addView(TextView(this).apply {
            text = "Photo & Signature Maker"
            textSize = 26f
            setTypeface(typeface, 1)
        })
        root.addView(TextView(this).apply {
            text = "Set size → choose image → preview → save"
            setPadding(0, 8, 0, 20)
        })
        w = EditText(this).apply { hint = "Width px (example 300)"; inputType = 2 }
        h = EditText(this).apply { hint = "Height px (example 400)"; inputType = 2 }
        root.addView(w); root.addView(h)
        root.addView(Button(this).apply {
            text = "Choose Image & Preview"
            setOnClickListener { startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 81) }
        })
        preview = ImageView(this).apply {
            adjustViewBounds = true
            visibility = View.GONE
            setPadding(0, 20, 0, 12)
        }
        root.addView(preview, LinearLayout.LayoutParams(-1, 520))
        save = Button(this).apply {
            text = "Save Image"
            visibility = View.GONE
            setOnClickListener {
                if (output == null) return@setOnClickListener
                val name = "EduNexa_Form_${System.currentTimeMillis()}.jpg"
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_TITLE, name)
                }, 82)
            }
        }
        root.addView(save)
        status = TextView(this).apply { setPadding(0, 16, 0, 0) }
        root.addView(status)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onActivityResult(q: Int, res: Int, data: Intent?) {
        super.onActivityResult(q, res, data)
        if (res != Activity.RESULT_OK) return
        if (q == 81) {
            val width = w.text.toString().toIntOrNull()
            val height = h.text.toString().toIntOrNull()
            if (width == null || height == null || width < 1 || height < 1) {
                status.text = "Enter valid width and height first"
                return
            }
            val uri = data?.data ?: return
            val bmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return
            output = Bitmap.createScaledBitmap(bmp, width, height, true)
            preview.setImageBitmap(output)
            preview.visibility = View.VISIBLE
            save.visibility = View.VISIBLE
            status.text = "Preview ready: ${width}×${height}px. Nothing has been saved yet."
        } else if (q == 82) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.openOutputStream(uri)?.use { output?.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                status.text = "Saved successfully"
            } catch (e: Exception) {
                status.text = e.message ?: "Save failed"
            }
        }
    }
}
