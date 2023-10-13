package com.example.mlkitlib.ocr.ui

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import com.example.mlkitlib.UnityBridge
import com.example.mlkitlib.databinding.ActivityCameraBinding
import com.example.mlkitlib.ocr.TextItem
import com.example.mlkitlib.ocr.TextRecognitionHelper
import com.example.mlkitlib.ocr.util.viewBinding
import com.example.mlkitlib.ocr.view.TextGraphic
import com.example.mlkitlib.ocr.view.TextRecognitionProcessor
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import resizeBitmap
import showToastShort
import uriToBitmap
import vibrate
import java.io.File


class CameraActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityCameraBinding::inflate)
    private val viewModel by viewModels<CameraViewModel>()

    private var imageCapture: ImageCapture? = null
    private var textHelper: TextRecognitionHelper? = null
    private var imageUri: Uri? = null

    private var rectList: MutableList<TextItem> = mutableListOf()

    private var PHOTO_FORMAT: String = ".jpg"

    private var isCameraUsing = false

    lateinit var mDetector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        mDetector = GestureDetectorCompat(this, MyGestureListener())

        initCameraX()
        setup()
    }

    private fun setup() {
        textHelper = TextRecognitionHelper()
        imageUri?.let { textHelper?.build(this) }

        binding.buttonDone.setOnClickListener {
            finish()
            UnityBridge.returnShow(binding.editTextResult.text.toString())
        }

        binding.buttonCancel.setOnClickListener{
            binding.layoutEdit.visibility = View.INVISIBLE
            setInitialScreen()
        }

        binding.buttonClose.setOnClickListener {
            binding.infoView.visibility = View.GONE
        }

        viewModel.selectedTextList.observe(this){ list ->
            binding.graphicOverlay.clear()
            var string = ""
            list.map {
                if(it.isSelected)
                    string += it.text
            }
            binding.editTextResult.setText(string)

            binding.graphicOverlay.add(
                viewModel.selectedTextList.value?.let { it1 ->
                    TextGraphic(binding.graphicOverlay,
                        it1,true,false,false)
                }
            )
        }
    }

    private fun setInitialScreen(){
        rectList.clear()
        binding.relativeResult.visibility = View.INVISIBLE
        binding.buttonTakePic.visibility = View.VISIBLE
        binding.cameraView.visibility = View.VISIBLE
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
            binding.infoView.visibility = View.GONE
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

        val imageProcessor = TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build(), viewModel.selectedTextList.value as List<TextItem>) { result, exception ->
            Log.d("TESTING", "recognizeText: value changed")

            result?.let { text ->
                viewModel.clearList()
                rectList.clear()
                val list = mutableListOf<TextItem>()
                text.textBlocks.map {
                    rectList.add(TextItem(it.text,it.lines.size, RectF(it.boundingBox),false))
                    list.add(TextItem(it.text,it.lines.size, RectF(it.boundingBox),false))
                }
                viewModel.setList(list)
                binding.graphicOverlay.setImageSourceInfo(imageBitmap.width, imageBitmap.height, false)
                updateUI(text)

            } ?: exception!!.localizedMessage?.let { UnityBridge.returnShow(it) }
        }
        imageProcessor.processBitmap(imageBitmap, binding.graphicOverlay)
    }

    // Update UI after captured photo
    private fun updateUI(result: Text) {
        binding.apply {
            layoutEdit.visibility = View.VISIBLE
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
        //    val rotatedBitmap = rotateBitmap(this, resizeBitmap)

            recognizeText(resizeBitmap)
        }
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return mDetector.onTouchEvent(event!!)
    }

    fun onTextClick(event: MotionEvent?){
        val x = event?.x
        val y = event?.y
        if (x != null && y != null)
            rectList.forEach {
                if (it.rect.contains(x,y)){
                    Log.d("TESTING", "onTouchEvent: you clicked ${(rectList.indexOf(it))+1}")
                    viewModel.handleClickedText(it)
                }
            }
    }

    inner class MyGestureListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onTextClick(e)
            return false
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