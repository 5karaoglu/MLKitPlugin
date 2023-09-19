package com.example.mlkitlib.ocr

import android.content.Context
import android.util.Log
import com.example.mlkitlib.ocr.view.TextRecognitionProcessor
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRecognitionHelper {

    private var imageProcessor: VisionImageProcessor? = null
    private var mContext: Context? = null

    fun build(context: Context) {
        mContext = context
        createImageProcessor()
    }

    fun resume() = createImageProcessor()

    fun pause() = imageProcessor?.run { this.stop() }

    fun destroy() = imageProcessor?.run { this.stop() }

    fun createImageProcessor() {
        imageProcessor = mContext?.let { TextRecognitionProcessor(it, TextRecognizerOptions.Builder().build()) { result, exception ->
            Log.d("TESTING", "createImageProcessor: value changed2")
        } }
    }
}