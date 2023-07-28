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
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.Camera.CameraInfo
import android.media.ExifInterface
import android.media.Image.Plane
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.huawei.hms.mlsdk.common.MLFrame
import java.io.*
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

    /** Converts a YUV_420_888 image from CameraX API to a bitmap.  */
    @RequiresApi(VERSION_CODES.LOLLIPOP)
    @ExperimentalGetImage
    fun getBitmap(image: ImageProxy): Bitmap? {
        val frameMetadata: FrameMetadata = FrameMetadata.Builder()
            .setWidth(image.width)
            .setHeight(image.height)
            .setRotation(image.imageInfo.rotationDegrees)
            .build()
        val nv21Buffer: ByteBuffer = yuv420ThreePlanesToNV21(image.image!!.planes, image.width, image.height)!!
        return getBitmap(nv21Buffer, frameMetadata)
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

    private fun yuv420ThreePlanesToNV21(
        yuv420888planes: Array<Plane>, width: Int, height: Int
    ): ByteBuffer? {
        val imageSize = width * height
        val out = ByteArray(imageSize + 2 * (imageSize / 4))
        if (areUVPlanesNV21(
                yuv420888planes,
                width,
                height
            )
        ) {
            // Copy the Y values.
            yuv420888planes[0].buffer[out, 0, imageSize]
            val uBuffer = yuv420888planes[1].buffer
            val vBuffer = yuv420888planes[2].buffer
            // Get the first V value from the V buffer, since the U buffer does not contain it.
            vBuffer[out, imageSize, 1]
            // Copy the first U value and the remaining VU values from the U buffer.
            uBuffer[out, imageSize + 1, 2 * imageSize / 4 - 1]
        } else {
            // Fallback to copying the UV values one by one, which is slower but also works.
            // Unpack Y.
            unpackPlane(
                yuv420888planes[0],
                width,
                height,
                out,
                0,
                1
            )
            // Unpack U.
            unpackPlane(
                yuv420888planes[1],
                width,
                height,
                out,
                imageSize + 1,
                2
            )
            // Unpack V.
            unpackPlane(
                yuv420888planes[2],
                width,
                height,
                out,
                imageSize,
                2
            )
        }
        return ByteBuffer.wrap(out)
    }


    /** Checks if the UV plane buffers of a YUV_420_888 image are in the NV21 format.  */
    private fun areUVPlanesNV21(planes: Array<Plane>, width: Int, height: Int): Boolean {
        val imageSize = width * height
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        // Backup buffer properties.
        val vBufferPosition = vBuffer.position()
        val uBufferLimit = uBuffer.limit()

        // Advance the V buffer by 1 byte, since the U buffer will not contain the first V value.
        vBuffer.position(vBufferPosition + 1)
        // Chop off the last byte of the U buffer, since the V buffer will not contain the last U value.
        uBuffer.limit(uBufferLimit - 1)

        // Check that the buffers are equal and have the expected number of elements.
        val areNV21 =
            vBuffer.remaining() == 2 * imageSize / 4 - 2 && vBuffer.compareTo(uBuffer) == 0

        // Restore buffers to their initial state.
        vBuffer.position(vBufferPosition)
        uBuffer.limit(uBufferLimit)
        return areNV21
    }

    /**
     * Unpack an image plane into a byte array.
     *
     *
     * The input plane data will be copied in 'out', starting at 'offset' and every pixel will be
     * spaced by 'pixelStride'. Note that there is no row padding on the output.
     */
    private fun unpackPlane(
        plane: Plane, width: Int, height: Int, out: ByteArray, offset: Int, pixelStride: Int
    ) {
        val buffer = plane.buffer
        buffer.rewind()

        // Compute the size of the current plane.
        // We assume that it has the aspect ratio as the original image.
        val numRow = (buffer.limit() + plane.rowStride - 1) / plane.rowStride
        if (numRow == 0) {
            return
        }
        val scaleFactor = height / numRow
        val numCol = width / scaleFactor

        // Extract the data in the output buffer.
        var outputPos = offset
        var rowStart = 0
        for (row in 0 until numRow) {
            var inputPos = rowStart
            for (col in 0 until numCol) {
                out[outputPos] = buffer[inputPos]
                outputPos += pixelStride
                inputPos += plane.pixelStride
            }
            rowStart += plane.rowStride
        }
    }

    @Throws(IOException::class)
    fun getBitmapFromContentUri(contentResolver: ContentResolver?, imageUri: Uri?): Bitmap? {
        val decodedBitmap =
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri) ?: return null
        val orientation: Int = getExifOrientationTag(
            contentResolver!!,
            imageUri!!
        )
        var rotationDegrees = 0
        var flipX = false
        var flipY = false
        when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipX = true
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotationDegrees = 90
            androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE -> {
                rotationDegrees = 90
                flipX = true
            }
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotationDegrees =
                180
            androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipY = true
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotationDegrees =
                -90
            androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE -> {
                rotationDegrees = -90
                flipX = true
            }
            androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED, androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL -> {}
            else -> {}
        }
        return rotateBitmap(
            decodedBitmap,
            rotationDegrees,
            flipX,
            flipY
        )
    }

    /** Rotates a bitmap if it is converted from a bytebuffer.  */
    private fun rotateBitmap(
        bitmap: Bitmap, rotationDegrees: Int, flipX: Boolean, flipY: Boolean
    ): Bitmap? {
        val matrix = Matrix()

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees.toFloat())

        // Mirror the image along the X or Y axis.
        matrix.postScale(if (flipX) -1.0f else 1.0f, if (flipY) -1.0f else 1.0f)
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
        }
        return rotatedBitmap
    }

    private fun getExifOrientationTag(resolver: ContentResolver, imageUri: Uri): Int {
        // We only support parsing EXIF orientation tag from local file on the device.
        // See also:
        // https://android-developers.googleblog.com/2016/12/introducing-the-exifinterface-support-library.html
        if (ContentResolver.SCHEME_CONTENT != imageUri.scheme
            && ContentResolver.SCHEME_FILE != imageUri.scheme
        ) {
            return 0
        }
        var exif: androidx.exifinterface.media.ExifInterface
        try {
            resolver.openInputStream(imageUri).use { inputStream ->
                if (inputStream == null) {
                    return 0
                }
                exif = androidx.exifinterface.media.ExifInterface(inputStream)
            }
        } catch (e: IOException) {
            Log.e(
                "BITMAP_TAG",
                "failed to open file to read rotation meta data: $imageUri", e
            )
            return 0
        }
        return exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )
    }
}