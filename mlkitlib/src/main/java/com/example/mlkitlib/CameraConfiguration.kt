package com.example.mlkitlib

import android.hardware.Camera


class CameraConfiguration {
    var fps = 26.0f
    var previewWidth = MAX_WIDTH
    var previewHeight = MAX_HEIGHT
    val isAutoFocus = true

    fun setCameraFacing(facing: Int) {
        synchronized(lock) {
            if ((facing != CameraConfiguration.CAMERA_FACING_BACK) && (facing != CameraConfiguration.CAMERA_FACING_FRONT)) {
                throw IllegalArgumentException("Invalid camera: $facing");
            }
            cameraFacing = facing
        }
    }

    companion object {
        const val CAMERA_FACING_BACK = Camera.CameraInfo.CAMERA_FACING_BACK
        const val CAMERA_FACING_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT
        const val DEFAULT_WIDTH = 640
        const val DEFAULT_HEIGHT = 360
        const val MAX_WIDTH = 1280
        const val MAX_HEIGHT = 720
        var cameraFacing = CAMERA_FACING_BACK
        private val lock = Any()

    }

    fun getCameraFacing(): Int {
        synchronized(lock) { return cameraFacing }
    }
}