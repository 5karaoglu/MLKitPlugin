package com.example.mlkitlib

import android.app.Activity
import android.util.Log
import com.example.mlkitlib.lensengine.LensEngine
import com.example.mlcardrecognitionsample.LensEnginePreview
import com.example.mlkitlib.lensengine.LocalFace3DTransactor
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.face.face3d.ML3DFace
import com.huawei.hms.mlsdk.face.face3d.ML3DFaceAnalyzer
import com.huawei.hms.mlsdk.face.face3d.ML3DFaceAnalyzerSetting
import java.io.IOException

class ThreeDFaceDetectionHelper {
    companion object{
        const val TAG = "ThreeDFaceDetectionHelper"
    }

    private var mActivity: Activity? = null

    private var lensEngine: LensEngine? = null

    private var mPreview: LensEnginePreview? = null

    private lateinit var analyzer: ML3DFaceAnalyzer


    private val cameraConfiguration: CameraConfiguration = CameraConfiguration()
    private val facing = CameraConfiguration.CAMERA_FACING_FRONT


    fun build(activity: Activity){
        mActivity = activity
        createLensEngine()

    }

    fun resume(){
        startLensEngine()
    }

    fun destroy(){
        if (this::analyzer.isInitialized) {
            try {
                analyzer.stop()
            } catch (e: IOException) {
                // Exception handling.
            }
        }
        lensEngine?.release()
    }



    private fun createLensEngine() {
        if (this.lensEngine == null) {
            this.lensEngine = mActivity?.let { LensEngine(it, cameraConfiguration) }
        }
        try {
            createFaceAnalyzer()
        } catch (e: Exception) {
            Log.e(
                TAG,
                "createLensEngine IOException." + e.message
            )
        }
    }

    private fun createFaceAnalyzer() {
        // Create a face analyzer. You can create an analyzer using the provided customized face detection parameter
        val setting = ML3DFaceAnalyzerSetting.Factory()
            .setPerformanceType(ML3DFaceAnalyzerSetting.TYPE_SPEED)
            .setTracingAllowed(true)
            .create()
        this.lensEngine?.setMachineLearningFrameTransactor(
            mActivity?.applicationContext?.let {
                LocalFace3DTransactor(
                    setting,
                    it
                )
            }
        )
    }

    private fun startLensEngine() {
        if (this.lensEngine != null) {
            try {
                this.mPreview?.start(this.lensEngine, true)
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "Unable to start lensEngine.",
                    e
                )
                this.lensEngine!!.release()
                this.lensEngine = null
            }
        }
    }

    fun threeDFaceDetection(): ML3DFaceAnalyzer {
        // Use custom parameter settings, and enable the speed preference mode and face tracking function to obtain a faster speed.
        val setting = ML3DFaceAnalyzerSetting.Factory() // Sets the preference mode of an analyzer.
            // ML3DFaceAnalyzerSetting.TYPE_SPEED: speed preference mode.
            // ML3DFaceAnalyzerSetting.TYPE_PRECISION: precision preference mode.
            .setPerformanceType(ML3DFaceAnalyzerSetting.TYPE_SPEED) // Set whether to enable face tracking in a specific mode.
            // Input parameter 1: true, indicating that the face tracking function is enabled.
            // Input parameter 1: false, indicating that the face tracking function is disabled.
            .setTracingAllowed(true)
            .create()
        analyzer = MLAnalyzerFactory.getInstance().get3DFaceAnalyzer(setting)
        (analyzer as ML3DFaceAnalyzer).setTransactor(ThreeDFaceAnalyzerTransactor())
        return analyzer as ML3DFaceAnalyzer
    }

}

class ThreeDFaceAnalyzerTransactor : MLAnalyzer.MLTransactor<ML3DFace?> {
    override fun transactResult(results: MLAnalyzer.Result<ML3DFace?>) {
        val items = results.analyseList
        // Determine detection result processing as required. Note that only the detection results are processed.
        // Other detection-related APIs provided by ML Kit cannot be called.
        Log.d("Thr", "transactResult: ${items[0]?.get3DFaceEulerY()} ")
    }

    override fun destroy() {
        // Callback method used to release resources when the detection ends.
    }
}
