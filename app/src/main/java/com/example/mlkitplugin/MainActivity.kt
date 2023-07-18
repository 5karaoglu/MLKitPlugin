package com.example.mlkitplugin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceView
import com.example.mlcardrecognitionsample.LensEnginePreview
import com.example.mlkitlib.ThreeDFaceDetectionHelper

class MainActivity : AppCompatActivity() {


    private var helper: ThreeDFaceDetectionHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val lensEnginePreview = findViewById<LensEnginePreview>(R.id.surface_view)
        helper = ThreeDFaceDetectionHelper()

        helper!!.build(this, lensEnginePreview)

    }

    override fun onResume() {
        super.onResume()
        helper?.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        helper?.destroy()
    }
}