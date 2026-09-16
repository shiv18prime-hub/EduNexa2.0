package com.edunexa.app.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageTools {
    fun resize(context: Context, source: Uri, width: Int, height: Int): File {
        require(width > 0 && height > 0) { "Width and height must be greater than zero" }
        val bitmap = context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Unable to open image" }
            BitmapFactory.decodeStream(input)
        } ?: error("Unsupported image")
        val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val output = File(context.cacheDir, "edunexa_resized_${System.currentTimeMillis()}.jpg")
        FileOutputStream(output).use { resized.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        if (resized !== bitmap) resized.recycle()
        bitmap.recycle()
        return output
    }

    fun compress(context: Context, source: Uri, quality: Int): File {
        require(quality in 1..100) { "Quality must be between 1 and 100" }
        val bitmap = context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Unable to open image" }
            BitmapFactory.decodeStream(input)
        } ?: error("Unsupported image")
        val output = File(context.cacheDir, "edunexa_compressed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(output).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        bitmap.recycle()
        return output
    }
}
