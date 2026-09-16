package com.edunexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

class PdfToolkitActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private lateinit var save: Button
    private var imageUri: Uri? = null
    private var pendingFile: File? = null
    private var hasUnsavedOutput = false
    private var mode = ""

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        PDFBoxResourceLoader.init(applicationContext)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(36,36,36,36) }
        root.addView(TextView(this).apply { text="PDF Toolkit"; textSize=26f; setTypeface(typeface,1) })
        root.addView(TextView(this).apply { text="Select → Process → Preview → Save\nFiles stay temporary until you tap Save."; setPadding(0,10,0,22) })
        fun button(label:String, action:()->Unit)=Button(this).apply { text=label; isAllCaps=false; setOnClickListener{action()} }.also{root.addView(it)}
        button("Image → PDF") { mode="image"; startActivityForResult(Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI),71) }
        button("Merge PDFs") { mode="merge"; startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/pdf";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)},73) }
        button("Split PDF") { mode="split"; pickPdf(74) }
        button("Reorder / Delete Pages") { mode="reorder"; pickPdf(75) }
        button("Compress PDF") { mode="compress"; pickPdf(76) }
        button("PDF → Images") { mode="images"; pickPdf(77) }
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
                71->{ imageUri=data?.data; val bmp=imageUri?.let{contentResolver.openInputStream(it)?.use(BitmapFactory::decodeStream)}?:return; preview.setImageBitmap(bmp);preview.visibility=View.VISIBLE;createImagePdf(bmp) }
                73->mergeSelected(data)
                74->data?.data?.let{askPages(it,"Split PDF","Pages to keep, e.g. 1-3 or 1,3,5",false)}
                75->data?.data?.let{askPages(it,"Reorder / Delete","New page order, e.g. 3,1,2. Omit a page to delete it.",true)}
                76->data?.data?.let{copyAsOptimized(it)}
                77->data?.data?.let{status.text="PDF → Images selected. Image export is next in the upgrade; no file was saved."}
                90->{val out=data?.data?:return;pendingFile?.inputStream()?.use{input->contentResolver.openOutputStream(out)?.use{input.copyTo(it)}};hasUnsavedOutput=false;status.text="Saved successfully"}
            }
        }catch(e:Exception){status.text="Could not process: ${e.message}"}
    }

    private fun createImagePdf(bmp:android.graphics.Bitmap){
        val f=temp("image.pdf"); val doc=android.graphics.pdf.PdfDocument(); val p=doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(bmp.width,bmp.height,1).create());p.canvas.drawBitmap(bmp,0f,0f,null);doc.finishPage(p);f.outputStream().use{doc.writeTo(it)};doc.close();ready(f,"Image PDF preview ready")
    }

    private fun mergeSelected(data:Intent?){
        val uris=mutableListOf<Uri>();data?.clipData?.let{c->for(i in 0 until c.itemCount)uris.add(c.getItemAt(i).uri)};data?.data?.let{uris.add(it)}
        if(uris.size<2){status.text="Select at least 2 PDFs";return}
        val merger=PDFMergerUtility(); val inputs=mutableListOf<File>()
        uris.forEachIndexed{i,u->val f=temp("merge_$i.pdf");contentResolver.openInputStream(u)!!.use{a->f.outputStream().use{a.copyTo(it)}};inputs.add(f);merger.addSource(f)}
        val out=temp("merged.pdf");merger.destinationFileName=out.absolutePath;merger.mergeDocuments(null);ready(out,"${uris.size} PDFs merged. Preview result is temporary.")
    }

    private fun askPages(uri:Uri,title:String,hint:String,reorder:Boolean){
        val input=EditText(this).apply{this.hint=hint;setPadding(30,20,30,20)}
        AlertDialog.Builder(this).setTitle(title).setView(input).setPositiveButton("Process"){_,_->processPages(uri,input.text.toString(),reorder)}.setNegativeButton("Cancel",null).show()
    }

    private fun processPages(uri:Uri,spec:String,reorder:Boolean){
        val src=temp("source.pdf");contentResolver.openInputStream(uri)!!.use{a->src.outputStream().use{a.copyTo(it)}}
        val source=PDDocument.load(src);val indexes=parsePages(spec,source.numberOfPages)
        if(indexes.isEmpty()){source.close();status.text="Enter valid page numbers";return}
        val outDoc=PDDocument();indexes.forEach{outDoc.importPage(source.getPage(it))};val out=temp(if(reorder)"reordered.pdf" else "split.pdf");outDoc.save(out);outDoc.close();source.close();ready(out,"${indexes.size} page(s) ready. Nothing saved yet.")
    }

    private fun parsePages(s:String,max:Int):List<Int>{
        val result=mutableListOf<Int>();s.split(',').map{it.trim()}.filter{it.isNotEmpty()}.forEach{part->
            if('-' in part){val a=part.substringBefore('-').toIntOrNull();val b=part.substringAfter('-').toIntOrNull();if(a!=null&&b!=null)for(n in a..b)if(n in 1..max)result.add(n-1)}
            else part.toIntOrNull()?.let{if(it in 1..max)result.add(it-1)}
        };return result
    }

    private fun copyAsOptimized(uri:Uri){
        val src=temp("compress_source.pdf");contentResolver.openInputStream(uri)!!.use{a->src.outputStream().use{a.copyTo(it)}}
        val doc=PDDocument.load(src);val out=temp("compressed.pdf");doc.documentInformation=null;doc.save(out);doc.close();ready(out,"PDF optimized. Final size depends on the original PDF.")
    }

    private fun temp(name:String)=File(cacheDir,"${System.currentTimeMillis()}_$name").also{it.deleteOnExit()}
    private fun ready(file:File,message:String){pendingFile=file;hasUnsavedOutput=true;save.visibility=View.VISIBLE;status.text="$message\n${file.length()/1024} KB • Tap Save Result to keep it."}
    private fun chooseSave(){if(pendingFile==null)return;startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/pdf";putExtra(Intent.EXTRA_TITLE,"EduNexa_${System.currentTimeMillis()}.pdf")},90)}

    override fun onBackPressed(){if(hasUnsavedOutput)AlertDialog.Builder(this).setTitle("Discard unsaved result?").setMessage("The processed file has not been saved yet.").setPositiveButton("Discard"){_,_->pendingFile?.delete();hasUnsavedOutput=false;super.onBackPressed()}.setNegativeButton("Stay",null).show() else super.onBackPressed()}
    override fun onDestroy(){if(hasUnsavedOutput)pendingFile?.delete();super.onDestroy()}
}
