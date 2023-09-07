package com.example.mlkitplugin

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mlcardrecognitionsample.LensEnginePreview
import com.example.mlkitlib.face.ThreeDFaceDetectionHelper
import com.example.mlkitlib.ocr.TextRecognitionHelper
import com.example.mlkitlib.ocr.TextRecognitionProcessor
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {


    private var helper: ThreeDFaceDetectionHelper? = null
    private var textHelper: TextRecognitionHelper? = null
    private var imageUri: Uri? = null
    private var textImagePreview: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val lensEnginePreview = findViewById<LensEnginePreview>(R.id.surface_view)
        val buttonCamera = findViewById<Button>(R.id.button_camera)
        textImagePreview = findViewById(R.id.textPreview)

        /**
         * 3D FaceDetection
         */
        helper = ThreeDFaceDetectionHelper()
        helper!!.build(this, lensEnginePreview)

        /**
         * Text Recognition
         */
        textHelper = TextRecognitionHelper()
        imageUri?.let { textHelper?.build(this) }

        buttonCamera.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePhoto.launch(cameraIntent)
        }
    }

    private fun recognizeText(imageBitmap: Bitmap) {
        textImagePreview?.setImageBitmap(imageBitmap)

        val imageProcessor = TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build()) {text, exception ->
            Log.d("TESTING", "recognizeText: value changed")
        }
        imageProcessor.processBitmap(imageBitmap)
    }

    private var takePhoto: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val rawUri = result.data?.extras?.get(DATA)
                val bitmap = rawUri as Bitmap
                recognizeText(bitmap)
            }
        }

    override fun onResume() {
        super.onResume()
        helper?.resume()
        textHelper?.resume()
    }

    override fun onPause() {
        super.onPause()
        textHelper?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        helper?.destroy()
        textHelper?.destroy()
    }

    companion object {
        const val DATA = "data"
    }
}