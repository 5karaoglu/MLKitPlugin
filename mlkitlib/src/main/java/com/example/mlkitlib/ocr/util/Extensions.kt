import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import java.io.IOException
import java.io.InputStream

fun uriToBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap? {
    var inputStream: InputStream? = null
    try {
        inputStream = contentResolver.openInputStream(uri)
        if (inputStream != null) {
            return BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    return null
}

fun resizeBitmap(bitmap: Bitmap): Bitmap {
    return Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
}
fun rotateBitmap(activity: Activity, bitmap: Bitmap): Bitmap {
    val matrix = Matrix()
    var rotationDegree = 0
    val orientation = activity.requestedOrientation

    if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) rotationDegree = 180
    else if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) rotationDegree = 90

    matrix.postRotate(rotationDegree.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun showToastShort(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()



