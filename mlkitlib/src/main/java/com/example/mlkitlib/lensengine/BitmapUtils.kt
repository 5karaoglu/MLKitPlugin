/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.mlkitlib.lensengine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera.CameraInfo
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import com.huawei.hms.mlsdk.common.MLFrame
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

object BitmapUtils {
    private const val TAG = "BitmapUtils"

    /**
     * Stretch the bitmap based on the given width and height
     *
     * @param origin    Original image
     * @param newWidth  Width of the new bitmap
     * @param newHeight Height of the new bitmap
     * @return new Bitmap
     */
    fun scaleBitmap(origin: Bitmap?, newWidth: Int, newHeight: Int): Bitmap? {
        val scaleWidth: Float
        val scaleHeight: Float
        if (origin == null) {
            return null
        }
        val height = origin.height
        val width = origin.width
        if (height > width) {
            scaleWidth = newWidth.toFloat() / width
            scaleHeight = newHeight.toFloat() / height
        } else {
            scaleWidth = newWidth.toFloat() / height
            scaleHeight = newHeight.toFloat() / width
        }
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false)
    }


    /**
     * Convert nv21 format byte buffer to bitmap
     *
     * @param data ByteBuffer data
     * @param metadata Frame meta data
     * @return Bitmap object
     */
    fun getBitmap(data: ByteBuffer, metadata: FrameMetadata): Bitmap? {
        data.rewind()
        val imageBuffer = ByteArray(data.limit())
        data[imageBuffer, 0, imageBuffer.size]
        try {
            val yuvImage = YuvImage(
                imageBuffer, ImageFormat.NV21, metadata.width,
                metadata.height, null
            )
            val stream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, metadata.width, metadata.height), 80, stream)
            val bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
            stream.close()
            return rotateBitmap(bitmap, metadata.rotation, metadata.cameraFacing)
        } catch (e: Exception) {
            Log.e(TAG, "Error: " + e.message)
        }
        return null
    }

    fun rotateBitmap(bitmap: Bitmap, rotation: Int, facing: Int): Bitmap {
        val matrix = Matrix()
        var rotationDegree = 0
        if (rotation == MLFrame.SCREEN_SECOND_QUADRANT) {
            rotationDegree = 90
        } else if (rotation == MLFrame.SCREEN_THIRD_QUADRANT) {
            rotationDegree = 180
        } else if (rotation == MLFrame.SCREEN_FOURTH_QUADRANT) {
            rotationDegree = 270
        }
        matrix.postRotate(rotationDegree.toFloat())
        if (facing != CameraInfo.CAMERA_FACING_BACK) {
            matrix.postScale(-1.0f, 1.0f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun recycleBitmap(vararg bitmaps: Bitmap?) {
        for (bitmap in bitmaps) {
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private fun getImagePath(activity: Activity, uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = activity.managedQuery(uri, projection, null, null, null)
        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(columnIndex)
    }

    fun loadFromPath(activity: Activity, uri: Uri, width: Int, height: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val path = getImagePath(activity, uri)
        BitmapFactory.decodeFile(path, options)
        val sampleSize = calculateInSampleSize(options, width, height)
        options.inSampleSize = sampleSize
        options.inJustDecodeBounds = false
        val bitmap = zoomImage(BitmapFactory.decodeFile(path, options), width, height)
        return rotateBitmap(bitmap, getRotationAngle(path))
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val width = options.outWidth
        val height = options.outHeight
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            // Calculate height and required height scale.
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            // Calculate width and required width scale.
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            // Take the larger of the values.
            inSampleSize = if (heightRatio > widthRatio) heightRatio else widthRatio
        }
        return inSampleSize
    }

    // Scale pictures to screen width.
    private fun zoomImage(imageBitmap: Bitmap, targetWidth: Int, maxHeight: Int): Bitmap {
        val scaleFactor = Math.max(
            imageBitmap.width.toFloat() / targetWidth.toFloat(),
            imageBitmap.height.toFloat() / maxHeight.toFloat()
        )
        return Bitmap.createScaledBitmap(
            imageBitmap,
            (imageBitmap.width / scaleFactor).toInt(),
            (imageBitmap.height / scaleFactor).toInt(),
            true
        )
    }

    /**
     * Get the rotation angle of the photo.
     *
     * @param path photo path.
     * @return angle.
     */
    fun getRotationAngle(path: String?): Int {
        var rotation = 0
        try {
            val exifInterface = ExifInterface(path!!)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotation = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> rotation = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> rotation = 270
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to get rotation: " + e.message)
        }
        return rotation
    }

    fun rotateBitmap(bitmap: Bitmap, angle: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        var result: Bitmap? = null
        try {
            result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Failed to rotate bitmap: " + e.message)
        }
        return result ?: bitmap
    }

    /**
     * Fusion of two images.
     * @param background background image.
     * @param foreground foreground image.
     * @return image.
     */
    fun joinBitmap(background: Bitmap?, foreground: Bitmap?): Bitmap? {
        if (background == null || foreground == null) {
            Log.e(TAG, "bitmap is null.")
            return null
        }
        if (background.height != foreground.height || background.width != foreground.width) {
            Log.e(TAG, "bitmap size is not match.")
            return null
        }
        val newmap =
            Bitmap.createBitmap(background.width, background.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newmap)
        canvas.drawBitmap(background, 0f, 0f, null)
        canvas.drawBitmap(foreground, 0f, 0f, null)
        canvas.save()
        canvas.restore()
        return newmap
    }

    fun saveToAlbum(bitmap: Bitmap, context: Context) {
        var file: File? = null
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val root = File(Environment.getExternalStorageDirectory().absoluteFile, context.packageName)
        val dir = File(root, "image")
        if (dir.mkdirs() || dir.isDirectory) {
            file = File(dir, fileName)
        }
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
        } catch (e: FileNotFoundException) {
            Log.e(TAG, e.message!!)
        } catch (e: IOException) {
            Log.e(TAG, e.message!!)
        } finally {
            try {
                os?.close()
            } catch (e: IOException) {
                Log.e(TAG, e.message!!)
            }
        }

        // Insert pictures into the system gallery.
        try {
            if (null != file) {
                MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    file.canonicalPath,
                    fileName,
                    null
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, e.message!!)
        }
        if (file == null) {
            return
        }
        // Gallery refresh.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            var path: String? = null
            try {
                path = file.canonicalPath
            } catch (e: IOException) {
                Log.e(TAG, e.message!!)
            }
            MediaScannerConnection.scanFile(
                context, arrayOf(path), null
            ) { path, uri ->
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = uri
                context.sendBroadcast(mediaScanIntent)
            }
        } else {
            val relationDir = file.parent
            val file1 = File(relationDir)
            context.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_MOUNTED,
                    Uri.fromFile(file1.absoluteFile)
                )
            )
        }
    }

    fun loadBitmapFromView(view: View, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.layout(0, 0, width, height)
        view.draw(canvas)
        return bitmap
    }
}