package com.example.qrscanfinalv2

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object ApiClient {

    private val client = OkHttpClient()

    // Change this depending on your setup:
    // Wi-Fi setup example:
    // private const val SERVER_URL = "http://192.168.1.5:8000/predict"
    //
    // USB + adb reverse setup:
    // private const val SERVER_URL = "http://127.0.0.1:8000/predict"
    private const val SERVER_URL = "http://192.168.1.5:8000/predict"

    fun uploadImage(
        imageFile: File,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",   // must match FastAPI endpoint parameter name
                        imageFile.name,
                        imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url(SERVER_URL)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    onSuccess(responseText)
                } else {
                    onError("Server error: $responseText")
                }

            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }.start()
    }
}