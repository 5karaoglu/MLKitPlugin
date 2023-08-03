package com.example.mlkitplugin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.mlkitlib.face.FaceActivity

class StarterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starter)

        val intent = Intent(this, FaceActivity::class.java)
        startActivity(intent)
    }
}