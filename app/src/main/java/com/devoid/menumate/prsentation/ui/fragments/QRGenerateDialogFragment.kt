package com.devoid.menumate.prsentation.ui.fragments

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.devoid.menumate.databinding.QrGenerateLayoutBinding
import com.devoid.menumate.utils.APP_LINK_URL
import com.devoid.menumate.utils.createPDFOfQRCodes
import com.devoid.menumate.utils.toBitmap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class QRGenerateDialogFragment : DialogFragment() {
    private lateinit var binding: QrGenerateLayoutBinding
    private val currentUser = Firebase.auth.currentUser


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = QrGenerateLayoutBinding.inflate(layoutInflater)
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create().apply {
                setOnShowListener {
                    init()
                }
            }
    }
   private fun init(){
        binding.apply {
            var qrGenerateJob:Job?=null
            generateBtn.setOnClickListener {
                lifecycleScope.launch {
                    val numberOfTables = noOfTables.text.toString().toIntOrNull()
                    if (numberOfTables != null && numberOfTables > 0) {
                        progressCircular.isVisible= true
                        noOfTables.isEnabled = false
                        generateBtn.visibility = View.GONE
                        qrGenerateJob= CoroutineScope(Dispatchers.IO).launch {
                            val pdfFile=createQrCodesFile(numberOfTables) { position ->
                                lifecycleScope.launch {
                                    noOfTables.setText("Processing $position/$numberOfTables")
                                }
                            }
                            withContext(Dispatchers.Main){
                                noOfTables.setText("Generated QR codes For $numberOfTables tables")
                                progressCircular.visibility = View.GONE
                                generateBtn.isVisible = true
                                //share pdf file
                                pdfFile?.let {
                                    generateBtn.apply {
                                        text = "Save"
                                        setOnClickListener{
                                            shareFile(requireContext(),pdfFile,"Save PDF File")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Invalid Input", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            cancelBtn.setOnClickListener{
                qrGenerateJob?.cancel()
                dismiss()
            }
        }

    }

    private suspend fun createQrCodesFile(
        noOfTables: Int,
        onProcess: (position: Int) -> Unit
    ): File? {
        val pdfTempFile :File?
        withContext(Dispatchers.IO) {
            ensureActive()
            val qrCodeWriter = QRCodeWriter()
            val bitmaps: Array<Bitmap?> = arrayOfNulls(noOfTables)
            for (i in 0 until noOfTables) {
                val content = "$APP_LINK_URL?id=${currentUser!!.uid}&table=${i + 1}"
                val qrBits = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 550, 550)
                bitmaps[i] = qrBits.toBitmap()
                onProcess(i)
            }
            ensureActive()
             pdfTempFile = com.devoid.menumate.utils.createTempFile("generated_qrs", ".pdf")
           // pdfTempFile.deleteOnExit()
            val pdfDocument = createPDFOfQRCodes(requireContext(), 149f, 200f, *bitmaps)
            try {
                FileOutputStream(pdfTempFile).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }finally {
                pdfDocument.close()
            }
        }
        return pdfTempFile
    }
    private fun shareFile(context: Context,file: File,message:String){
        val sharingIntent= Intent()
        sharingIntent.action= Intent.ACTION_SEND
        sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri= FileProvider.getUriForFile(context,"com.devoid.menumate.fileprovider",file)
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sharingIntent.setDataAndType(uri,context.contentResolver.getType(uri))
        sharingIntent.putExtra(Intent.EXTRA_STREAM,uri)
        context.startActivity(Intent.createChooser(sharingIntent,message))
    }
}