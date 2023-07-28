package com.example.mlkitlib.lensengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.util.Log
import com.example.mlkitlib.CameraConfiguration
import com.huawei.hmf.tasks.Task
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.face.MLFace
import com.huawei.hms.mlsdk.face.face3d.ML3DFace
import java.nio.ByteBuffer


abstract class BaseTransactor<T> : ImageTransactor {
    // To keep the latest images and its metadata.
    private var latestImage: ByteBuffer? = null
    private var latestImageMetaData: FrameMetadata? = null

    // To keep the images and metadata in process.
    private var transactingImage: ByteBuffer? = null
    private var transactingMetaData: FrameMetadata? = null
    private var mContext: Context? = null
    private var converter: NV21ToBitmapConverter? = null

    constructor() {}
    constructor(context: Context?) {
        mContext = context
        converter = NV21ToBitmapConverter(mContext)
    }

    @Synchronized
    override fun process(
        data: ByteBuffer,
        frameMetadata: FrameMetadata
    ) {
        latestImage = data
        latestImageMetaData = frameMetadata
        if (transactingImage == null && transactingMetaData == null) {
            processLatestImage()
        }
    }

    override fun process(bitmap: Bitmap) {
        val frame = MLFrame.Creator().setBitmap(bitmap).create()
        detectInVisionImage(bitmap, frame, null)
    }

    @Synchronized
    private fun processLatestImage() {
        transactingImage = latestImage
        transactingMetaData = latestImageMetaData
        latestImage = null
        latestImageMetaData = null
        var bitmap: Bitmap? = null
        if (transactingImage != null && transactingMetaData != null) {
            val width: Int
            val height: Int
            width = transactingMetaData!!.width
            height = transactingMetaData!!.height
            val metadata = MLFrame.Property.Creator().setFormatType(ImageFormat.NV21)
                .setWidth(width)
                .setHeight(height)
                .setQuadrant(transactingMetaData!!.rotation)
                .create()
            if (isFaceDetection) {
                Log.d(TAG, "Total HMSFaceProc getBitmap start")
                bitmap = converter!!.getBitmap(transactingImage, transactingMetaData)
                Log.d(TAG, "Total HMSFaceProc getBitmap end")
                val resizeBitmap: Bitmap = BitmapUtils.scaleBitmap(
                    bitmap, CameraConfiguration.DEFAULT_HEIGHT,
                    CameraConfiguration.DEFAULT_WIDTH
                )!!
                Log.d(TAG, "Total HMSFaceProc resizeBitmap end")
                detectInVisionImage(
                    bitmap, MLFrame.fromBitmap(resizeBitmap),
                    transactingMetaData
                )
            } else {
                bitmap = BitmapUtils.getBitmap(
                    transactingImage!!, transactingMetaData!!
                )
                detectInVisionImage(
                    bitmap, MLFrame.fromByteBuffer(transactingImage, metadata),
                    transactingMetaData
                )
            }
        }
    }

    private fun detectInVisionImage(
        bitmap: Bitmap?, image: MLFrame, metadata: FrameMetadata?
    ) {
        detectInImage(image).addOnSuccessListener { results ->
            if (metadata == null || metadata.cameraFacing == CameraConfiguration.cameraFacing) {
                this@BaseTransactor.onSuccess(bitmap, results as T, metadata)
            }
            processLatestImage()
        }.addOnFailureListener { e -> this@BaseTransactor.onFailure(e) }
    }

    override fun stop() {}

    /**
     * Detect image
     *
     * @param image MLFrame object
     * @return Task object
     */
    protected abstract fun detectInImage(image: MLFrame?): Task<T>

    /**
     * Callback that executes with a successful detection result.
     *
     * @param originalCameraImage hold the original image from camera, used to draw the background image.
     * @param results T object
     * @param frameMetadata FrameMetadata object
     * @param graphicOverlay GraphicOverlay object
     */
    protected abstract fun onSuccess(
        originalCameraImage: Bitmap?,
        results: T,
        frameMetadata: FrameMetadata?
    )

    /**
     * Callback that executes with failure detection result.
     *
     * @param exception Exception object
     */
    protected abstract fun onFailure(exception: Exception)
    override val isFaceDetection: Boolean
        get() = false

    companion object {
        private const val TAG = "BaseTransactor"
    }
}