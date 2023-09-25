package com.example.mlkitlib

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.example.mlkitlib.ocr.ui.TextActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object UnityBridge {
    private val TAG = UnityBridge::class.java.simpleName
    private var callback: ResultListener? = null

    @OptIn(DelicateCoroutinesApi::class)
    fun receiveShow(activity: Activity, callback: ResultListener?) {
        GlobalScope.launch(Dispatchers.Main){
            Log.d(TAG, "receiveShow: step0")
            UnityBridge.callback = callback
            Log.d(TAG, "receiveShow: step1")
            TextActivity.start(activity)
            Log.d(TAG, "receiveShow: step2")
        }
    }

    fun returnShow(text: String) {
        Log.d(TAG, "[HMS] returnShow")
        callback?.onSuccess(text)
    }
}