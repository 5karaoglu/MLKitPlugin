package com.example.mlkitlib.ocr

import android.app.Activity
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
import com.example.mlkitlib.R
import com.example.mlkitlib.ResultListener
import com.example.mlkitlib.UnityBridge
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

const val TYPE = "TYPE"
class TextActivity : AppCompatActivity() {
    companion object {
        const val DATA = "data"
        const val TAG = "TESTING"
        @OptIn(DelicateCoroutinesApi::class)
        fun start(activity: Activity) {
            GlobalScope.launch(Dispatchers.Main){
                val intent: Intent = Intent(activity, TextActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }


    private var textHelper: TextRecognitionHelper? = null
    private var imageUri: Uri? = null
    private var textImagePreview: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)

        val buttonCamera = findViewById<Button>(R.id.button_camera)
        textImagePreview = findViewById(R.id.textPreview)

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

        val imageProcessor = TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build()) { result, exception ->
            Log.d("TESTING", "recognizeText: value changed")
            if (result != null){
                finish()
                UnityBridge.returnShow(result.text)
            }
            else
                exception!!.localizedMessage?.let { UnityBridge.returnShow(it) }
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