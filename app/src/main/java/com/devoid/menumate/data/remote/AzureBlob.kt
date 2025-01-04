package com.devoid.menumate.data.remote

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException


private val client = OkHttpClient()
const val TAG = "AureBlob"
const val SERVER_ADDR = "192.168.1.7"
const val RESTAURANT_ID = "cPnkeQPZlEhNc9CsDDR3r9lObtp1"
const val TABLENO=1

suspend fun uploadBlob(file: File, path: String, name: String): Task<Void> {
    val taskCompletionSource = TaskCompletionSource<Void>()
        val url = HttpUrl.Builder()
            .scheme("http")
            .host(SERVER_ADDR)
            .port(3000)
            .addPathSegments("upload")
            .addQueryParameter("filePath", path)
            .build()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                name,
                file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            )
            .build()
        // Create the request
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        withContext(Dispatchers.IO) {
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        taskCompletionSource.setException(e)
                    }

                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        taskCompletionSource.setResult(null)
                    }
                })
        }
    return taskCompletionSource.task
}