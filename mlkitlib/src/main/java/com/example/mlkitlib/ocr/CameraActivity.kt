package com.example.mlkitlib.ocr

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.mlkitlib.R
import java.io.File

class CameraActivity : AppCompatActivity() {

    private var cameraView: PreviewView? = null
    private var buttonTakePic: Button? = null
    private var imageCapture: ImageCapture? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        cameraView = findViewById(R.id.cameraView)
        buttonTakePic = findViewById(R.id.button_take_pic)

        initCameraX()
    }

    private fun initCameraX() {
        // CameraX'i başlat
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        imageCapture = ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .build()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Kamerayı bağla
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(cameraView?.surfaceProvider)

            // Kamerayı yaşam döngüsüne bağla
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))

        takePic()
    }

    private fun takePic() {
        // Fotoğraf çekme işlemi

        buttonTakePic?.setOnClickListener {
            val photoFile = File(externalMediaDirs.firstOrNull(), "${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture?.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("Error when image capture", exc.localizedMessage)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val resultIntent = Intent()
                        val savedUri = Uri.fromFile(photoFile)

                        resultIntent.putExtra("SAVED_URI", savedUri.toString())
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                })
        }
    }
}