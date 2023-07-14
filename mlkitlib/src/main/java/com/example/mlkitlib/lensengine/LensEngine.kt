package com.example.mlkitlib.lensengine

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.hardware.Camera.PreviewCallback
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.mlkitlib.CameraConfiguration
import com.huawei.hms.common.size.Size
import java.io.IOException
import java.nio.ByteBuffer
import java.util.IdentityHashMap


/**
 * Manages the camera and allows UI updates on top of it (e.g. overlaying extra Graphics or
 * displaying extra information). This receives preview frames from the camera at a specified rate,
 * sending those frames to child classes' detectors / classifiers as fast as it is able to process.
 *
 * @since 2019-12-26
 */
@SuppressLint("MissingPermission")
class LensEngine(
    protected var activity: Activity,
    configuration: CameraConfiguration?
) {
    @get:Synchronized
    var camera: Camera? = null
        private set
    private var transactingThread: Thread? = null
    private val transactingRunnable: FrameTransactingRunnable
    private val transactorLock = Any()
    private var frameTransactor: ImageTransactor? = null
    private val selector: CameraSelector
    private val bytesToByteBuffer: MutableMap<ByteArray, ByteBuffer> = IdentityHashMap()
    private val overlay: Any? = null

    init {
        transactingRunnable = FrameTransactingRunnable()
        selector = CameraSelector(activity, configuration!!)
    }

    /**
     * Stop the camera and release the resources of the camera and analyzer.
     */
    fun release() {
        synchronized(transactorLock) {
            stop()
            transactingRunnable.release()
            if (frameTransactor != null) {
                frameTransactor!!.stop()
                frameTransactor = null
            }
        }
    }

    /**
     * Turn on the camera and start sending preview frames to the analyzer for detection.
     *
     * @throws IOException IO Exception
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.CAMERA)
    @Synchronized
    @Throws(
        IOException::class
    )
    fun run(): LensEngine {
        if (camera != null) {
            return this
        }
        camera = createCamera()
        camera!!.startPreview()
        transactingThread = Thread(transactingRunnable)
        transactingRunnable.setActive(true)
        transactingThread!!.start()
        return this
    }

    /**
     * Take pictures.
     *
     * @param pictureCallback  Callback function after obtaining photo data.
     */
    @Synchronized
    fun takePicture(pictureCallback: PictureCallback?) {
        synchronized(transactorLock) {
            if (camera != null) {
                camera!!.takePicture(null, null, null, pictureCallback)
            }
        }
    }

    /*private fun initializeOverlay() {
        if (overlay != null) {
            val min: Int
            val max: Int
            if (frameTransactor?.isFaceDetection == true) {
                min = CameraConfiguration.DEFAULT_HEIGHT
                max = CameraConfiguration.DEFAULT_WIDTH
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    overlay.setCameraInfo(min, max, 0)
                } else {
                    overlay.setCameraInfo(max, min, 0)
                }
            } else {
                val size = previewSize
                min = Math.min(size.width, size.height)
                max = Math.max(size.width, size.height)
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    overlay.setCameraInfo(min, max, facing)
                } else {
                    overlay.setCameraInfo(max, min, facing)
                }
            }
            overlay.clear()
        }
    }*/

    /**
     * Get camera preview size.
     *
     * @return Size Size of camera preview.
     */
    val previewSize: Size
        get() = selector.previewSize!!
    val facing: Int
        get() = selector.facing!!

    /**
     * Turn off the camera and stop transmitting frames to the analyzer.
     */
    @Synchronized
    fun stop() {
        transactingRunnable.setActive(false)
        if (transactingThread != null) {
            try {
                transactingThread!!.join()
            } catch (e: InterruptedException) {
                Log.d(TAG, "Frame transacting thread interrupted on release.")
            }
            transactingThread = null
        }
        if (camera != null) {
            camera!!.stopPreview()
            camera!!.setPreviewCallbackWithBuffer(null)
            try {
                camera!!.setPreviewDisplay(null)
                camera!!.setPreviewTexture(null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear camera preview: $e")
            }
            camera!!.release()
            camera = null
        }
        bytesToByteBuffer.clear()
    }

    @SuppressLint("InlinedApi")
    @Throws(IOException::class)
    private fun createCamera(): Camera {
        val newCamera: Camera = selector.createCamera()
        newCamera.setPreviewCallbackWithBuffer(CameraPreviewCallback())
        newCamera.addCallbackBuffer(createPreviewBuffer(selector.previewSize!!))
        newCamera.addCallbackBuffer(createPreviewBuffer(selector.previewSize!!))
        newCamera.addCallbackBuffer(createPreviewBuffer(selector.previewSize!!))
        newCamera.addCallbackBuffer(createPreviewBuffer(selector.previewSize!!))
        return newCamera
    }

    /**
     * Create a buffer for the camera preview callback. The size of the buffer is based on the camera preview size and the camera image format.
     *
     * @param previewSize Preview size
     * @return Image data from the camera
     */
    @SuppressLint("InlinedApi")
    private fun createPreviewBuffer(previewSize: Size): ByteArray {
        val bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21)
        val sizeInBits = previewSize.height.toLong() * previewSize.width * bitsPerPixel
        val bufferSize = Math.ceil(sizeInBits / 8.0).toInt() + 1
        val byteArray = ByteArray(bufferSize)
        val buffer = ByteBuffer.wrap(byteArray)
        if (!buffer.hasArray() || (!buffer.array().contentEquals(byteArray))) {
            throw IllegalStateException("Failed to create valid buffer for lensEngine.") }
        bytesToByteBuffer[byteArray] = buffer
        return byteArray
    }

    private inner class CameraPreviewCallback : PreviewCallback {
        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            transactingRunnable.setNextFrame(data, camera)
        }
    }

    fun setMachineLearningFrameTransactor(transactor: ImageTransactor?) {
        synchronized(transactorLock) {
            if (frameTransactor != null) {
                frameTransactor!!.stop()
            }
            frameTransactor = transactor
        }
    }

    /**
     * It is used to receive the frame captured by the camera and pass it to the analyzer.
     */
    private inner class FrameTransactingRunnable internal constructor() : Runnable {
        private val lock = Object()
        private var active = true
        private var pendingFrameData: ByteBuffer? = null

        /**
         * Frees the transactor and can safely perform this operation only after the associated thread has completed.
         */
        @SuppressLint("Assert")
        fun release() {
            synchronized(lock) { assert(transactingThread!!.state == Thread.State.TERMINATED) }
        }

        fun setActive(active: Boolean) {
            synchronized(lock) {
                this.active = active
                lock.notifyAll()
            }
        }

        /**
         * Sets the frame data received from the camera. Adds a previously unused frame buffer (if exit) back to the camera.
         */
        fun setNextFrame(data: ByteArray, camera: Camera) {
            synchronized(lock) {
                if (pendingFrameData != null) {
                    camera.addCallbackBuffer(pendingFrameData!!.array())
                    pendingFrameData = null
                }
                if (!bytesToByteBuffer.containsKey(data)) {
                    Log.d(
                        TAG,
                        "Skipping frame. Could not find ByteBuffer associated with the image "
                                + "data from the camera."
                    )
                    return
                }
                pendingFrameData = bytesToByteBuffer[data]
                lock.notifyAll()
            }
        }

        @SuppressLint("InlinedApi")
        override fun run() {
            var data: ByteBuffer?
            while (true) {
                synchronized(lock) {
                    while (active && this.pendingFrameData == null) {
                        try {
                            // Waiting for next frame.
                            lock.wait()
                        } catch (e: InterruptedException) {
                            Log.w(
                                TAG,
                                "Frame transacting loop terminated.",
                                e
                            )
                            return
                        }
                    }
                    if (!active) {
                        pendingFrameData = null
                        return
                    }
                    data = pendingFrameData
                    pendingFrameData = null
                }
                try {
                    synchronized(transactorLock) {
                        Log.d(TAG, "Process an image")
                        frameTransactor!!.process(
                            data!!,
                            FrameMetadata.Builder()
                                .setWidth(selector.previewSize!!.getWidth())
                                .setHeight(selector.previewSize!!.getHeight())
                                .setRotation(selector.rotation)
                                .setCameraFacing(selector.facing)
                                .build()
                        )
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Exception thrown from receiver.", t)
                } finally {
                    camera!!.addCallbackBuffer(data!!.array())
                }
            }
        }
    }

    companion object {
        private const val TAG = "LensEngine"
    }
}