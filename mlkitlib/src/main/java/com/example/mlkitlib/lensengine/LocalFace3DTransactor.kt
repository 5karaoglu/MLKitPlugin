package com.example.mlkitlib.lensengine

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.huawei.hmf.tasks.Task
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.face.face3d.ML3DFace
import com.huawei.hms.mlsdk.face.face3d.ML3DFaceAnalyzer
import com.huawei.hms.mlsdk.face.face3d.ML3DFaceAnalyzerSetting
import java.io.IOException


class LocalFace3DTransactor(setting: ML3DFaceAnalyzerSetting?, context: Context) :
    BaseTransactor<List<ML3DFace?>?>(context) {
    private val detector: ML3DFaceAnalyzer
    private val mContext: Context

    init {
        detector = MLAnalyzerFactory.getInstance().get3DFaceAnalyzer(setting)
        mContext = context
    }

    override fun stop() {
        try {
            detector.stop()
        } catch (e: IOException) {
            Log.e(TAG, "Exception thrown while trying to close face transactor: " + e.message)
        }
    }

    override fun detectInImage(image: MLFrame?): Task<List<ML3DFace?>?> {
        return this.detector.asyncAnalyseFrame(image)
    }

    override fun onFailure(exception: Exception) {
        Log.d("toby", "Total HMSFaceProc graphicOverlay onFailure")
        Log.e(TAG, "Face detection failed: " + exception?.message)
    }

    override fun onSuccess(
        originalCameraImage: Bitmap?,
        results: List<ML3DFace?>?,
        frameMetadata: FrameMetadata?
    ) {
        Log.d("toby", "Total HMSFaceProc graphicOverlay start")
        if (results != null) {
            if (results.isNotEmpty())
                Log.d("Thr", "transactResult: ${results?.get(0)?.get3DAllVertexs()?.get(0)?.x},${results?.get(0)?.get3DAllVertexs()?.get(0)?.y}," +
                        "${results?.get(0)?.get3DAllVertexs()?.get(0)?.z} ")
        }
      /*  if (originalCameraImage != null) {
            val imageGraphic = CameraImageGraphic(graphicOverlay, originalCameraImage)
            graphicOverlay.addGraphic(imageGraphic)
        }
        Log.d("toby", "Total HMSFaceProc hmsMLLocalFaceGraphic start")
        val hmsML3DLocalFaceGraphic = Local3DFaceGraphic(graphicOverlay, results, mContext)
        graphicOverlay.addGraphic(hmsML3DLocalFaceGraphic)
        graphicOverlay.postInvalidate()*/
        Log.d("toby", "Total HMSFaceProc graphicOverlay end")
    }

    override val isFaceDetection: Boolean
        get() = true

    companion object {
        private const val TAG = "LocalFaceTransactor"
    }
}
