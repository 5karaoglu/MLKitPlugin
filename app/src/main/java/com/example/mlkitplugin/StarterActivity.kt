package com.example.mlkitplugin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.mlkitlib.ResultListener
import com.example.mlkitlib.UnityBridge
import com.example.mlkitlib.face.FaceActivity
import com.example.mlkitlib.ocr.TextActivity

class StarterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starter)

        UnityBridge.receiveShow(this, object : ResultListener{
            override fun onSuccess(text: String) {
                Log.d("TESTING", "onSuccess: ")
            }

            override fun onFailure(e: String) {
                Log.d("TESTING", "onFail: ")
            }

        })
    }
}