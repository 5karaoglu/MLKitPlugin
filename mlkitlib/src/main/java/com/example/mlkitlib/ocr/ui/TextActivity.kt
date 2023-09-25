package com.example.mlkitlib.ocr.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mlkitlib.R
import com.example.mlkitlib.databinding.ActivityCameraBinding
import com.example.mlkitlib.databinding.ActivityTextBinding
import com.example.mlkitlib.ocr.util.viewBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TextActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityTextBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.buttonCamera.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }

    companion object {
        const val TAG = "TESTING"

        @OptIn(DelicateCoroutinesApi::class)
        fun start(activity: Activity) {
            GlobalScope.launch(Dispatchers.Main) {
                val intent = Intent(activity, TextActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }
}