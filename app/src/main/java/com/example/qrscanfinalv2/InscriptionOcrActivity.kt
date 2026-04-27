package com.example.qrscanfinalv2

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class InscriptionOcrActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var imgPreview: ImageView
    private lateinit var imgCrop: ImageView
    private lateinit var txtResult: TextView

    private var imageCapture: ImageCapture? = null
    private val client = OkHttpClient()

    companion object {
        private const val REQ_CAMERA = 1001

        // Same Wi-Fi testing:
        //private const val API_URL = "http://192.168.0.100:8000/predict"
        private const val API_URL = "http://64.227.154.148/brahmi/predict"

        // If using adb reverse instead, use:
        // private const val API_URL = "http://127.0.0.1:8000/predict"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inscription_ocr)

        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnCapture)
        imgPreview = findViewById(R.id.imgPreview)
        imgCrop = findViewById(R.id.imgCrop)
        txtResult = findViewById(R.id.txtResult)

        btnCapture.setOnClickListener {
            capturePhoto()
        }

        if (hasCameraPermission()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQ_CAMERA
            )
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Camera start failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val ic = imageCapture
        if (ic == null) {
            Toast.makeText(this, "Camera is not ready yet", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFile = File(
            externalCacheDir,
            "inscription_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(System.currentTimeMillis())
            }.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        ic.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@InscriptionOcrActivity,
                        "Capture failed: ${exc.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    imgPreview.setImageURI(null)
                    imgPreview.setImageURI(Uri.fromFile(photoFile))

                    // For now, same image shown in crop preview as placeholder.
                    // Later you can replace this with actual cropped letter patch if needed.
                    imgCrop.setImageURI(null)
                    imgCrop.setImageURI(Uri.fromFile(photoFile))

                    txtResult.text = "Result: Processing..."

                    val t0 = SystemClock.elapsedRealtime()

                    uploadToApi(photoFile) { label, sinhala, confidence, reason, error ->
                        runOnUiThread {
                            val ms = SystemClock.elapsedRealtime() - t0

                            if (error != null) {
                                txtResult.text = "ERROR: $error"
                            } else {
                                txtResult.text =
                                    "Letter: $label\n" +
                                            "Sinhala: $sinhala\n" +
                                            "Confidence: ${"%.3f".format(confidence)}\n" +
                                            "Reason: $reason\n" +
                                            "Time: ${ms}ms"
                            }
                        }
                    }
                }
            }
        )
    }

    private fun uploadToApi(
        file: File,
        callback: (String, String, Double, String, String?) -> Unit
    ) {
        val mediaType = "image/jpeg".toMediaType()

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",   // Must match FastAPI parameter: file: UploadFile = File(...)
                file.name,
                file.asRequestBody(mediaType)
            )
            .build()

        val request = Request.Builder()
            .url(API_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback("", "", 0.0, "", e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                val text = response.body?.string()

                if (!response.isSuccessful || text == null) {
                    callback("", "", 0.0, "", "Server error: ${response.code}")
                    return
                }

                try {
                    val json = JSONObject(text)

                    val label = json.optString("label", "unknown")
                    val sinhala = json.optString("sinhala", "Unknown")
                    val confidence = json.optDouble("confidence", 0.0)
                    val reason = json.optString("reason", "")

                    callback(label, sinhala, confidence, reason, null)

                } catch (ex: Exception) {
                    callback("", "", 0.0, "", "Bad JSON: ${ex.message}\nRaw=$text")
                }
            }
        })
    }
}