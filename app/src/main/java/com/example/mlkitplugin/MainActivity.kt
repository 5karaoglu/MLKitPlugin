package com.example.mlkitplugin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.mlkitlib.ThreeDFaceDetectionHelper

class MainActivity : AppCompatActivity() {


    private var helper: ThreeDFaceDetectionHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        helper = ThreeDFaceDetectionHelper()

        helper!!.build(this)

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