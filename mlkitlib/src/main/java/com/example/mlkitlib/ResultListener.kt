package com.example.mlkitlib

import com.google.mlkit.vision.text.Text

interface ResultListener<in T> {
    fun onSuccess(text: T)
    fun onFailure(e: Exception)
}

