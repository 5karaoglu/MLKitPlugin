package com.example.mlkitlib.face

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import androidx.core.util.isNotEmpty
import com.example.mlkitlib.R
import com.example.mlkitlib.ResultListener
import com.google.mlkit.vision.text.Text
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.face.face3d.ML3DFace

class FaceActivity : AppCompatActivity() {
    val TAG = "FaceActivity"

    private var faceDetector: ThreeDFaceDetectionHelperEasy? = null
    private var surfaceView: SurfaceView? = null

    var resultListener: ResultListener<MLAnalyzer.Result<ML3DFace?>?>? = null

    fun start(rl: ResultListener<MLAnalyzer.Result<ML3DFace?>?>){
        resultListener = rl
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face)
        surfaceView = findViewById(R.id.surface_view)

       faceDetector = ThreeDFaceDetectionHelperEasy()
        faceDetector!!.buildAndStartDetection(this,surfaceView!!,transactor)
    }

    private val transactor = object : FaceTransactor{
        override fun destroy() {
            Log.d(TAG, "destroy: Transactor destroyed.")
        }

        override fun transactResult(p0: MLAnalyzer.Result<ML3DFace?>?) {
            if (p0 != null) {
                if (p0.analyseList.isNotEmpty()){
                    resultListener?.onSuccess(p0)
                }else{
                    resultListener?.onFailure(Exception("No results"))
                }
                    Log.d(TAG, "transactResult: ${p0?.analyseList?.get(0)?.get3DAllVertexs()?.size}")
            }else{
                resultListener?.onFailure(Exception("Result is null!"))
            }
        }

    }

    override fun onDestroy() {
        faceDetector?.stopDetection()
        super.onDestroy()
    }
}