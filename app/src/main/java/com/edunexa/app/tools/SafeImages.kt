package com.edunexa.app.tools
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
object SafeImages {
 fun decode(context:Context,uri:Uri,maxSide:Int=1800):Bitmap{val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)};require(bounds.outWidth>0&&bounds.outHeight>0){"Choose a readable image"};var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>maxSide)sample*=2;return context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888})}?:error("Could not read image")}
}
