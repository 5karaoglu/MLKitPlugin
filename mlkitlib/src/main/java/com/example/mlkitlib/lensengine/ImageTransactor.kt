package com.example.mlkitlib.lensengine

import android.graphics.Bitmap
import java.nio.ByteBuffer


interface ImageTransactor {
    /**
     * Start detection
     *
     * @param data ByteBuffer object
     * @param frameMetadata FrameMetadata object
     * @param graphicOverlay GraphicOverlay object
     */
    fun process(data: ByteBuffer, frameMetadata: FrameMetadata)

    /**
     * Start detection
     *
     * @param bitmap Bitmap object
     * @param graphicOverlay GraphicOverlay object
     */
    fun process(bitmap: Bitmap)

    /**
     * Stop detection
     */
    fun stop()

    /**
     * Is it face detection?
     *
     * @return boolean value
     */
    val isFaceDetection: Boolean
}