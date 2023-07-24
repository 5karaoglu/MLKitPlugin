package com.example.mlkitlib.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.widget.ImageView
import com.example.mlkitlib.lensengine.BitmapUtils
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

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
        imageProcessor = mContext?.let { TextRecognitionProcessor(it, TextRecognizerOptions.Builder().build()) }
    }
}