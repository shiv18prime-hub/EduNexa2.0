package com.edunexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.max

class ImageMergeActivity : AppCompatActivity() {
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var save: Button
    private lateinit var layoutText: TextView
    private var images = mutableListOf<Bitmap>()
    private var resultBytes: ByteArray? = null
    private var unsaved = false
    private var columns = 2
    private var gap = 8

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(color: String) = GradientDrawable().apply {
        setColor(Color.parseColor(color)); cornerRadius = dp(16).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(30))
            setBackgroundColor(Color.parseColor("#F6F7FC"))
        }
        root.addView(TextView(this).apply { text = "Image Merge Studio"; textSize = 26f; setTypeface(typeface, 1); setTextColor(Color.parseColor("#17213D")) })
        root.addView(TextView(this).apply { text = "Select • arrange layout • edit spacing • preview • save"; setTextColor(Color.parseColor("#72798F")); setPadding(0, dp(5), 0, dp(14)) })
        root.addView(Button(this).apply {
            text = "Select Images"; isAllCaps = false
            setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) }, 301) }
        })
        root.addView(TextView(this).apply { text = "EDIT LAYOUT"; setTypeface(typeface, 1); setTextColor(Color.parseColor("#3157D5")); setPadding(0, dp(16), 0, dp(6)) })
        layoutText = TextView(this).apply { text = "Grid: 2 columns • Gap: 8 px" }
        root.addView(layoutText)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (n in 1..3) controls.addView(Button(this).apply { text = "$n Column${if (n > 1) "s" else ""}"; isAllCaps = false; setOnClickListener { columns = n; render() } }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(controls)
        root.addView(TextView(this).apply { text = "Spacing" })
        root.addView(SeekBar(this).apply {
            max = 60; progress = 8
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { gap = progress; if (fromUser) render() }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        })
        root.addView(TextView(this).apply { text = "LIVE PREVIEW"; setTypeface(typeface, 1); setTextColor(Color.parseColor("#3157D5")); setPadding(0, dp(12), 0, dp(6)) })
        preview = ImageView(this).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER; background = bg("#FFFFFF") }
        root.addView(preview, LinearLayout.LayoutParams(-1, dp(480)))
        save = Button(this).apply { text = "Save Merged Image"; isAllCaps = false; visibility = View.GONE; setOnClickListener { chooseSave() } }
        root.addView(save)
        status = TextView(this).apply { text = "Nothing is saved until you tap Save."; setPadding(0, dp(10), 0, 0) }
        root.addView(status)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun render() {
        if (images.size < 2) return
        columns = columns.coerceIn(1, images.size)
        runCatching {
            val merged = mergeGrid(images, columns, gap)
            val out = ByteArrayOutputStream()
            merged.compress(Bitmap.CompressFormat.JPEG, 92, out)
            resultBytes = out.toByteArray()
            preview.setImageBitmap(merged)
            save.visibility = View.VISIBLE
            unsaved = true
            layoutText.text = "Grid: $columns column${if (columns > 1) "s" else ""} • Gap: $gap px"
            status.text = "${images.size} images • Editable preview ready • ${resultBytes!!.size / 1024} KB"
        }.onFailure { status.text = "Could not render images: ${it.message}" }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        if (requestCode == 301) {
            val uris = mutableListOf<Uri>()
            data?.clipData?.let { clip -> for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri) }
            data?.data?.let { uris.add(it) }
            images.forEach { if (!it.isRecycled) it.recycle() }
            images = uris.take(12).mapNotNull { decodeSampled(it, 1800) }.toMutableList()
            if (images.size < 2) { status.text = "Select at least 2 valid images"; save.visibility = View.GONE; return }
            render()
        } else if (requestCode == 302) {
            val uri = data?.data ?: return
            val bytes = resultBytes ?: return
            runCatching { contentResolver.openOutputStream(uri)?.use { it.write(bytes) }; unsaved = false; status.text = "Merged image saved successfully" }
                .onFailure { status.text = "Save failed: ${it.message}" }
        }
    }

    private fun decodeSampled(uri: Uri, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun mergeGrid(src: List<Bitmap>, cols: Int, spacing: Int): Bitmap {
        val rows = ceil(src.size.toDouble() / cols).toInt()
        val cell = 600
        val out = Bitmap.createBitmap(cell * cols + spacing * (cols + 1), cell * rows + spacing * (rows + 1), Bitmap.Config.RGB_565)
        val canvas = Canvas(out); canvas.drawColor(Color.WHITE)
        src.forEachIndexed { index, bitmap ->
            val scale = max(cell.toFloat() / bitmap.width, cell.toFloat() / bitmap.height)
            val sw = (bitmap.width * scale).toInt(); val sh = (bitmap.height * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(bitmap, sw, sh, true)
            val col = index % cols; val row = index / cols
            val left = spacing + col * (cell + spacing); val top = spacing + row * (cell + spacing)
            canvas.save(); canvas.clipRect(left, top, left + cell, top + cell)
            canvas.drawBitmap(scaled, left + (cell - sw) / 2f, top + (cell - sh) / 2f, null); canvas.restore()
            if (scaled !== bitmap) scaled.recycle()
        }
        return out
    }

    private fun chooseSave() {
        if (resultBytes == null) return
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "image/jpeg"; putExtra(Intent.EXTRA_TITLE, "EduNexa_Merge_${System.currentTimeMillis()}.jpg") }, 302)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (unsaved) AlertDialog.Builder(this).setTitle("Discard merged image?").setMessage("This edited preview has not been saved.").setPositiveButton("Discard") { _, _ -> unsaved = false; super.onBackPressed() }.setNegativeButton("Stay", null).show() else super.onBackPressed()
    }
}
