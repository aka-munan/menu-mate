package com.devoid.menumate.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.media.Image
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.devoid.menumate.R
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.BitMatrix
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

val APP_LINK_URL="https://devoid.web.app/menumate"

interface ResultCallback<T> {
    fun onResult(result: T) {
    }
}

fun createTempFile(name: String, ext: String): File {
    return File.createTempFile(name, ext)
}

fun readContentFile(context: Context, uri: Uri): InputStream? {
    if (!uri.scheme.equals("content"))
        return null
    return context.contentResolver.openInputStream(uri)
}

fun saveImageToTmpFile(context: Context, name: String, ext: String, uri: Uri, quality: Int): Uri? {
    val tmpFile = File.createTempFile(name, ext)
    tmpFile.deleteOnExit()
    val ips = readContentFile(context, uri)
    val ops = tmpFile.outputStream()
    val buffer = ByteArray(1024)
    if (quality > 0) {
        val bitmap = BitmapFactory.decodeStream(ips)
        val bao = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bao)
        val baips = ByteArrayInputStream(bao.toByteArray())
        while (baips.read(buffer) > 0) {
            ops.write(buffer)
        }
        baips.close()
    } else {
        while (ips!!.read(buffer) > 0) {
            ops.write(buffer)
        }
    }
    ips!!.close()
    ops.close()
    Log.i("Compression", "saveToTmpFile: compressed size: ${tmpFile.length()}")
    return Uri.fromFile(tmpFile)
}

fun BitMatrix.toBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bmp.setPixel(x, y, if (get(x, y)) Color.BLACK else Color.WHITE)
        }
    }
    return bmp
}

 fun Image.toLuminanceSource(): PlanarYUVLuminanceSource {
    val plane = planes[0]
    val buffer = plane.buffer
    val data = ByteArray(buffer.capacity())
    buffer.get(data)

    return PlanarYUVLuminanceSource(
        data,
        width,
        height,
        0,
        0,
        width,
        height,
        false
    )
}
 fun createPDFOfQRCodes(context: Context,x:Float,y:Float,vararg bitmaps: Bitmap?): PdfDocument {
    val pdfDocument = PdfDocument()
     val paint=Paint().apply {
         color = Color.BLACK
         textSize = 120f
         textAlign = Paint.Align.CENTER

     }

    bitmaps.forEach {

        if (it == null) {
            return@forEach
        }
        val tableNo= bitmaps.indexOf(it) + 1
        //a2 size doc
        val pageInfo = PageInfo.Builder(1191, 1684,tableNo ).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        ContextCompat.getDrawable(context,R.drawable.background)?.let {
            it.setBounds(0,0,pageInfo.pageWidth,pageInfo.pageHeight)
            it.draw(canvas)
        }
        canvas.drawText("Table $tableNo ", (pageInfo.pageWidth/2).toFloat(),150f,paint)
        canvas.drawBitmap(it,Rect(0,0,it.width,it.height),RectF(x,y,pageInfo.pageWidth-x,pageInfo.pageWidth-x),null)
        pdfDocument.finishPage(page)
    }
    return pdfDocument
}


