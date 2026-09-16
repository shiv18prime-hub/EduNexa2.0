package com.edunexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

class PdfToolkitActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private lateinit var save: Button
    private var pendingFile: File? = null
    private var pendingImages = mutableListOf<File>()
    private var hasUnsavedOutput = false
    private var outputKind = "pdf"

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        PDFBoxResourceLoader.init(applicationContext)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(36,36,36,36) }
        root.addView(TextView(this).apply { text="PDF Toolkit"; textSize=26f; setTypeface(typeface,1) })
        root.addView(TextView(this).apply { text="Select → Process → Preview → Save\nNothing is saved permanently until you tap Save."; setPadding(0,10,0,22) })
        fun button(label:String, action:()->Unit)=Button(this).apply { text=label; isAllCaps=false; setOnClickListener{action()} }.also{root.addView(it)}
        button("Image → PDF") { startActivityForResult(Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI),71) }
        button("Merge PDFs") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/pdf";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)},73) }
        button("Split PDF") { pickPdf(74) }
        button("Reorder / Delete Pages") { pickPdf(75) }
        button("Compress PDF") { pickPdf(76) }
        button("PDF → Images") { pickPdf(77) }
        preview=ImageView(this).apply{adjustViewBounds=true;visibility=View.GONE}; root.addView(preview,LinearLayout.LayoutParams(-1,480))
        save=Button(this).apply{text="Save Result";isAllCaps=false;visibility=View.GONE;setOnClickListener{chooseSave()}};root.addView(save)
        status=TextView(this).apply{gravity=Gravity.CENTER;setPadding(0,20,0,10)};root.addView(status)
        setContentView(ScrollView(this).apply{addView(root)})
    }

    private fun pickPdf(code:Int)=startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/pdf";addCategory(Intent.CATEGORY_OPENABLE)},code)

    override fun onActivityResult(q:Int,res:Int,data:Intent?){
        super.onActivityResult(q,res,data); if(res!=Activity.RESULT_OK)return
        try{
            when(q){
                71->{val uri=data?.data?:return;val bmp=contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?:return;preview.setImageBitmap(bmp);preview.visibility=View.VISIBLE;createImagePdf(bmp)}
                73->mergeSelected(data)
                74->data?.data?.let{askPages(it,"Split PDF","Pages to keep, e.g. 1-3 or 1,3,5",false)}
                75->data?.data?.let{askPages(it,"Reorder / Delete","New page order, e.g. 3,1,2. Omit a page to delete it.",true)}
                76->data?.data?.let{compressPdf(it)}
                77->data?.data?.let{pdfToImages(it)}
                90->{val out=data?.data?:return;pendingFile?.inputStream()?.use{input->contentResolver.openOutputStream(out)?.use{input.copyTo(it)}};hasUnsavedOutput=false;status.text="Saved successfully";save.visibility=View.GONE}
                91->{val tree=data?.data?:return;saveImagesToFolder(tree)}
            }
        }catch(e:Exception){status.text="Could not process: ${e.message}"}
    }

    private fun createImagePdf(bmp:Bitmap){
        val f=temp("image.pdf"); val doc=PdfDocument(); val p=doc.startPage(PdfDocument.PageInfo.Builder(bmp.width,bmp.height,1).create());p.canvas.drawBitmap(bmp,0f,0f,null);doc.finishPage(p);f.outputStream().use{doc.writeTo(it)};doc.close();readyPdf(f,"Image PDF preview ready")
    }

    private fun mergeSelected(data:Intent?){
        val uris=mutableListOf<Uri>();data?.clipData?.let{c->for(i in 0 until c.itemCount)uris.add(c.getItemAt(i).uri)};data?.data?.let{uris.add(it)}
        if(uris.size<2){status.text="Select at least 2 PDFs";return}
        val merger=PDFMergerUtility()
        uris.forEachIndexed{i,u->val f=temp("merge_$i.pdf");contentResolver.openInputStream(u)!!.use{a->f.outputStream().use{a.copyTo(it)}};merger.addSource(f)}
        val out=temp("merged.pdf");merger.destinationFileName=out.absolutePath;merger.mergeDocuments(null);readyPdf(out,"${uris.size} PDFs merged. Result is temporary.")
    }

    private fun askPages(uri:Uri,title:String,hint:String,reorder:Boolean){
        val input=EditText(this).apply{this.hint=hint;setPadding(30,20,30,20)}
        AlertDialog.Builder(this).setTitle(title).setView(input).setPositiveButton("Process"){_,_->processPages(uri,input.text.toString(),reorder)}.setNegativeButton("Cancel",null).show()
    }

    private fun processPages(uri:Uri,spec:String,reorder:Boolean){
        val src=copyToTemp(uri,"source.pdf");val source=PDDocument.load(src);val indexes=parsePages(spec,source.numberOfPages)
        if(indexes.isEmpty()){source.close();status.text="Enter valid page numbers";return}
        val outDoc=PDDocument();indexes.forEach{outDoc.importPage(source.getPage(it))};val out=temp(if(reorder)"reordered.pdf" else "split.pdf");outDoc.save(out);outDoc.close();source.close();readyPdf(out,"${indexes.size} page(s) ready. Nothing saved yet.")
    }

    private fun parsePages(s:String,max:Int):List<Int>{
        val result=mutableListOf<Int>();s.split(',').map{it.trim()}.filter{it.isNotEmpty()}.forEach{part->
            if('-' in part){val a=part.substringBefore('-').toIntOrNull();val b=part.substringAfter('-').toIntOrNull();if(a!=null&&b!=null&&a<=b)for(n in a..b)if(n in 1..max)result.add(n-1)}
            else part.toIntOrNull()?.let{if(it in 1..max)result.add(it-1)}
        };return result
    }

    private fun compressPdf(uri:Uri){
        status.text="Compressing PDF..."
        val src=copyToTemp(uri,"compress_source.pdf")
        val renderer=PdfRenderer(ParcelFileDescriptor.open(src,ParcelFileDescriptor.MODE_READ_ONLY))
        val out=temp("compressed.pdf");val doc=PdfDocument()
        for(i in 0 until renderer.pageCount){
            val page=renderer.openPage(i);val scale=minOf(1f,1240f/page.width.toFloat());val w=maxOf(1,(page.width*scale).toInt());val h=maxOf(1,(page.height*scale).toInt())
            val bmp=Bitmap.createBitmap(w,h,Bitmap.Config.RGB_565);bmp.eraseColor(Color.WHITE);page.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            val pdfPage=doc.startPage(PdfDocument.PageInfo.Builder(w,h,i+1).create());pdfPage.canvas.drawBitmap(bmp,0f,0f,null);doc.finishPage(pdfPage);bmp.recycle();page.close()
        }
        out.outputStream().use{doc.writeTo(it)};doc.close();renderer.close()
        readyPdf(out,"Compressed sharing copy ready. Pages are flattened to reduce size.")
    }

    private fun pdfToImages(uri:Uri){
        status.text="Creating image previews...";clearPending();val src=copyToTemp(uri,"images_source.pdf");val renderer=PdfRenderer(ParcelFileDescriptor.open(src,ParcelFileDescriptor.MODE_READ_ONLY))
        for(i in 0 until renderer.pageCount){
            val page=renderer.openPage(i);val scale=minOf(2f,1600f/page.width.toFloat());val w=maxOf(1,(page.width*scale).toInt());val h=maxOf(1,(page.height*scale).toInt());val bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bmp.eraseColor(Color.WHITE);page.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            val f=temp("page_${i+1}.jpg");f.outputStream().use{bmp.compress(Bitmap.CompressFormat.JPEG,92,it)};if(i==0){preview.setImageBitmap(bmp.copy(Bitmap.Config.ARGB_8888,false));preview.visibility=View.VISIBLE};bmp.recycle();page.close();pendingImages.add(f)
        }
        renderer.close();outputKind="images";hasUnsavedOutput=pendingImages.isNotEmpty();save.text="Save Images";save.visibility=if(hasUnsavedOutput)View.VISIBLE else View.GONE;status.text="${pendingImages.size} image(s) ready. Tap Save Images and choose a folder."
    }

    private fun saveImagesToFolder(tree:Uri){
        val folder=DocumentFile.fromTreeUri(this,tree)?:throw IllegalStateException("Folder unavailable")
        pendingImages.forEachIndexed{i,f->val dest=folder.createFile("image/jpeg","EduNexa_Page_${i+1}.jpg")?:return@forEachIndexed;f.inputStream().use{a->contentResolver.openOutputStream(dest.uri)?.use{a.copyTo(it)}}}
        hasUnsavedOutput=false;status.text="${pendingImages.size} image(s) saved successfully";save.visibility=View.GONE
    }

    private fun copyToTemp(uri:Uri,name:String):File{val f=temp(name);contentResolver.openInputStream(uri)!!.use{a->f.outputStream().use{a.copyTo(it)}};return f}
    private fun temp(name:String)=File(cacheDir,"${System.currentTimeMillis()}_$name")
    private fun readyPdf(file:File,message:String){clearPending();pendingFile=file;outputKind="pdf";hasUnsavedOutput=true;save.text="Save Result";save.visibility=View.VISIBLE;status.text="$message\n${file.length()/1024} KB • Tap Save Result to keep it."}
    private fun chooseSave(){
        if(outputKind=="images"){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),91);return}
        if(pendingFile==null)return;startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/pdf";putExtra(Intent.EXTRA_TITLE,"EduNexa_${System.currentTimeMillis()}.pdf")},90)
    }
    private fun clearPending(){pendingFile?.delete();pendingFile=null;pendingImages.forEach{it.delete()};pendingImages.clear();hasUnsavedOutput=false}

    override fun onBackPressed(){if(hasUnsavedOutput)AlertDialog.Builder(this).setTitle("Discard unsaved result?").setMessage("The processed result has not been saved yet.").setPositiveButton("Discard"){_,_->clearPending();super.onBackPressed()}.setNegativeButton("Stay",null).show() else super.onBackPressed()}
    override fun onDestroy(){if(hasUnsavedOutput)clearPending();super.onDestroy()}
}
