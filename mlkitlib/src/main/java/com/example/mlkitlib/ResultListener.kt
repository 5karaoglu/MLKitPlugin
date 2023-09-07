package com.example.mlkitlib

interface ResultListener {
    fun onSuccess(text: String)
    fun onFailure(e: String)
}

