package com.example.mlkitlib.face

import android.app.Activity
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.face.face3d.ML3DFace
import com.huawei.hms.mlsdk.face.face3d.ML3DFaceAnalyzer
import com.huawei.hms.mlsdk.face.face3d.ML3DFaceAnalyzerSetting
import java.io.IOException


class ThreeDFaceDetectionHelperEasy(){
    companion object{
        const val TAG = "ThreeDFaceDetectionHelperEasy"
    }

    private var mActivity: Activity? = null
    private var analyzer: ML3DFaceAnalyzer? = null
    private var lensEngine: LensEngine? = null
    private var mSurfaceView: SurfaceView? = null

    fun buildAndStartDetection(activity: Activity, surfaceView: SurfaceView, transactor: MLAnalyzer.MLTransactor<ML3DFace?>){
        mActivity = activity
        mSurfaceView = surfaceView
        analyzer = threeDFaceDetection(transactor)

        lensEngine = LensEngine.Creator(mActivity!!.applicationContext, analyzer)
            .setLensType(LensEngine.BACK_LENS)
            .applyDisplayDimension(1440, 1080)
            .applyFps(30.0f)
            .enableAutomaticFocus(true)
            .create()

        mSurfaceView!!.holder.addCallback(object : SurfaceHolder.Callback{
            override fun surfaceCreated(holder: SurfaceHolder) {
                startDetection()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }
        })
    }

    private fun threeDFaceDetection(transactor: MLAnalyzer.MLTransactor<ML3DFace?>): ML3DFaceAnalyzer {
        // Use custom parameter settings, and enable the speed preference mode and face tracking function to obtain a faster speed.
        val setting = ML3DFaceAnalyzerSetting.Factory() // Sets the preference mode of an analyzer.
            // ML3DFaceAnalyzerSetting.TYPE_SPEED: speed preference mode.
            // ML3DFaceAnalyzerSetting.TYPE_PRECISION: precision preference mode.
            .setPerformanceType(ML3DFaceAnalyzerSetting.TYPE_SPEED) // Set whether to enable face tracking in a specific mode.
            // Input parameter 1: true, indicating that the face tracking function is enabled.
            // Input parameter 1: false, indicating that the face tracking function is disabled.
            .setTracingAllowed(true)
            .create()
        val analyzer = MLAnalyzerFactory.getInstance().get3DFaceAnalyzer(setting)
        analyzer.setTransactor(transactor)
        return analyzer
    }

    private fun startDetection(){
        try {
            lensEngine!!.run(mSurfaceView!!.holder)
        } catch (e: IOException) {
            // Exception handling logic.
            Log.e(TAG, "onCreate: ${e.message}")
            e.printStackTrace()
        }
    }


    fun stopDetection(){
        if (analyzer != null) {
            try {
                analyzer?.stop()
            } catch (e: IOException) {
                // Exception handling.
            }
        }
        lensEngine?.release()
    }
}

open class FaceAnalyzerTransactor : MLAnalyzer.MLTransactor<ML3DFace?> {
    override fun transactResult(results: MLAnalyzer.Result<ML3DFace?>) {
        val items = results.analyseList
        // Determine detection result processing as required. Note that only the detection results are processed.
        // Other detection-related APIs provided by ML Kit cannot be called.
    }

    override fun destroy() {
        // Callback method used to release resources when the detection ends.
    }
}