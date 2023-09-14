package com.example.mlkitlib.ocr

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.net.toUri
import com.example.mlkitlib.R
import com.example.mlkitlib.UnityBridge
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import resizeBitmap
import rotateBitmap
import uriToBitmap


const val TYPE = "TYPE"
class TextActivity : AppCompatActivity() {
    companion object {
        const val DATA = "data"
        const val TAG = "TESTING"
        @OptIn(DelicateCoroutinesApi::class)
        fun start(activity: Activity) {
            GlobalScope.launch(Dispatchers.Main){
                val intent = Intent(activity, TextActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }


    private var textHelper: TextRecognitionHelper? = null
    private var imageUri: Uri? = null

    private var textImagePreview: ImageView? = null
    private var editTextResult: AppCompatEditText? = null
    private var buttonSubmitResult: AppCompatImageButton? = null
    private var layoutEdit: RelativeLayout? = null

    private var graphicOverlay: GraphicOverlay? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)

        val buttonCamera = findViewById<Button>(R.id.button_camera)
        textImagePreview = findViewById(R.id.textPreview)
        editTextResult = findViewById(R.id.edit_text_result)
        buttonSubmitResult = findViewById(R.id.button_submit)
        layoutEdit = findViewById(R.id.layout_edit)
        graphicOverlay = findViewById(R.id.graphic_overlay)

        /**
         * Text Recognition
         */
        textHelper = TextRecognitionHelper()
        imageUri?.let { textHelper?.build(this) }

        buttonCamera.setOnClickListener {
            val cameraIntent = Intent(this,CameraActivity::class.java)
            takePhoto.launch(cameraIntent)
        }

        buttonSubmitResult?.setOnClickListener {
            finish()
            UnityBridge.returnShow(editTextResult?.text.toString())
        }
    }

    private fun recognizeText(imageBitmap: Bitmap) {
        runOnUiThread {
            textImagePreview?.setImageBitmap(imageBitmap)
        }

        graphicOverlay?.clear()
        val imageProcessor = TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build()) { result, exception ->
            Log.d("TESTING", "recognizeText: value changed")
            if (result != null){
                graphicOverlay?.setImageSourceInfo(imageBitmap.width, imageBitmap.height,false)
                layoutEdit?.visibility = View.VISIBLE
                editTextResult?.setText(result.text)
                editTextResult?.requestFocus()
            }
            else
                exception!!.localizedMessage?.let { UnityBridge.returnShow(it) }
        }
        imageProcessor.processBitmap(imageBitmap, graphicOverlay!!)
    }

    var takePhoto: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val receivedData = result.data?.getStringExtra("SAVED_URI")

                val bitmap = uriToBitmap(contentResolver,receivedData)

                if (bitmap != null) {
                    val resizeBitmap = resizeBitmap(bitmap)
                    val rotatedBitmap = rotateBitmap(this,resizeBitmap)

                    recognizeText(rotatedBitmap)
                }
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