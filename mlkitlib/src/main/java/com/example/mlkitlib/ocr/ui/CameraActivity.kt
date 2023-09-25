package com.example.mlkitlib.ocr.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.mlkitlib.R
import com.example.mlkitlib.UnityBridge
import com.example.mlkitlib.databinding.ActivityCameraBinding
import com.example.mlkitlib.ocr.TextRecognitionHelper
import com.example.mlkitlib.ocr.util.viewBinding
import com.example.mlkitlib.ocr.view.GraphicOverlay
import com.example.mlkitlib.ocr.view.TextRecognitionProcessor
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import resizeBitmap
import rotateBitmap
import showToastShort
import uriToBitmap
import vibrate
import java.io.File


class CameraActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityCameraBinding::inflate)

    private var imageCapture: ImageCapture? = null
    private var textHelper: TextRecognitionHelper? = null
    private var imageUri: Uri? = null

    private var PHOTO_FORMAT: String = ".jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initCameraX()
        setup()
    }

    private fun setup() {
        textHelper = TextRecognitionHelper()
        imageUri?.let { textHelper?.build(this) }

        binding.buttonSubmit.setOnClickListener {
            finish()
            UnityBridge.returnShow(binding.editTextResult.text.toString())
        }
    }

    private fun initCameraX() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .build()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
            preview.setSurfaceProvider(binding.cameraView.surfaceProvider)

            // Kamerayı lifecycle'a bağla
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))

        takePhoto()
    }

    private fun takePhoto() {
        binding.buttonTakePic.setOnClickListener {
            val photoFile = File(externalMediaDirs.firstOrNull(), "${System.currentTimeMillis()}$PHOTO_FORMAT")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()


            imageCapture?.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        configBitmap(savedUri)
                        vibrate(this@CameraActivity)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e("Error when image capture", exc.localizedMessage)
                        showToastShort(this@CameraActivity,"Error when capture, ${exc.localizedMessage}")
                    }
                })
        }
    }

    private fun recognizeText(imageBitmap: Bitmap) {
        runOnUiThread { binding.textPreview.setImageBitmap(imageBitmap) }
        binding.graphicOverlay.clear()

        val imageProcessor = TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build()) { result, exception ->
            Log.d("TESTING", "recognizeText: value changed")

            result?.let {
                binding.graphicOverlay.setImageSourceInfo(imageBitmap.width, imageBitmap.height, false)
                updateUI(it)

            } ?: exception!!.localizedMessage?.let { UnityBridge.returnShow(it) }
        }
        imageProcessor.processBitmap(imageBitmap, binding.graphicOverlay)
    }

    // Update UI after captured photo
    private fun updateUI(result: Text) {
        binding.apply {
            layoutEdit.visibility = View.VISIBLE
            editTextResult.setText(result.text)
            editTextResult.requestFocus()
        }
    }

    private fun configBitmap(uri: Uri) {
        val bitmap = uriToBitmap(contentResolver, uri)

        if (bitmap != null) {
            binding.relativeResult.visibility = View.VISIBLE
            binding.buttonTakePic.visibility = View.GONE
            binding.cameraView.visibility = View.GONE

            val resizeBitmap = resizeBitmap(bitmap)
            val rotatedBitmap = rotateBitmap(this, resizeBitmap)

            recognizeText(rotatedBitmap)
        }
    }

    override fun onResume() {
        super.onResume()
        textHelper?.resume()
    }

    override fun onPause() {
        super.onPause()
        textHelper?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        textHelper?.destroy()
    }

}