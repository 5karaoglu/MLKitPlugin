package com.example.mlkitlib.face

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import com.example.mlkitlib.R
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.face.face3d.ML3DFace

class FaceActivity : AppCompatActivity() {
    val TAG = "FaceActivity"

    private var faceDetector: ThreeDFaceDetectionHelperEasy? = null
    private var surfaceView: SurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face)
        surfaceView = findViewById(R.id.surface_view)

       faceDetector = ThreeDFaceDetectionHelperEasy()
        faceDetector!!.buildAndStartDetection(this,surfaceView!!,transactor)
    }

    private val transactor = object : MLAnalyzer.MLTransactor<ML3DFace?>{
        override fun destroy() {
            Log.d(TAG, "destroy: Transactor destroyed.")
        }

        override fun transactResult(p0: MLAnalyzer.Result<ML3DFace?>?) {
            Log.d(TAG, "transactResult: ${p0?.analyseList?.get(0)?.get3DAllVertexs()?.size}")
        }

    }

    override fun onDestroy() {
        faceDetector?.stopDetection()
        super.onDestroy()
    }
}