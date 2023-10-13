package com.example.mlkitlib.ocr.view

import android.content.Context
import android.graphics.RectF
import android.util.Log
import com.example.mlkitlib.ocr.TextItem
import com.example.mlkitlib.ocr.util.PreferenceUtils
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface

class TextRecognitionProcessor(private val context: Context, textRecognizerOptions: TextRecognizerOptionsInterface,
                               private val selectedList: List<TextItem> = listOf(),
private val callbackListener: (Text?, Exception?) -> Unit
)
    : VisionProcessorBase<Text>(context) {

    private val textRecognizer: TextRecognizer = TextRecognition.getClient(textRecognizerOptions)
    private val shouldGroupRecognizedTextInBlocks: Boolean =
        true
    private val showLanguageTag: Boolean = PreferenceUtils.showLanguageTag(context)
    private val showConfidence: Boolean = PreferenceUtils.shouldShowTextConfidence(context)

    override fun stop() {
        super.stop()
        textRecognizer.close()
    }

    override fun detectInImage(image: InputImage): Task<Text> {
        return textRecognizer.process(image)
    }

    override fun onSuccess(results: Text,graphicOverlay: GraphicOverlay) {
        Log.d(TAG, "On-device Text detection successful")
        logExtrasForTesting(results)
        val list = mutableListOf<TextItem>()
        if (selectedList.isEmpty())
            results.textBlocks.forEach {
                val rectF = RectF(it.boundingBox)
                list.add(TextItem(it.text,it.lines.size,rectF,false))
            }
        else
            selectedList.forEach {
                list.add(it)
            }
        /*graphicOverlay.clear()
        graphicOverlay.add(
            TextGraphic(
                graphicOverlay,
                list,
                shouldGroupRecognizedTextInBlocks,
                showLanguageTag,
                showConfidence
            )
        )*/
        callbackListener(results, null)
    }

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Text detection failed.$e")
        callbackListener(null, e)
    }

    companion object {
        private const val TAG = "TextRecProcessor"
        private fun logExtrasForTesting(text: Text?) {
            if (text != null) {
                Log.v(MANUAL_TESTING_LOG, "Detected text has : " + text.textBlocks.size + " blocks")
                for (i in text.textBlocks.indices) {
                    val lines = text.textBlocks[i].lines
                    Log.v(
                        MANUAL_TESTING_LOG,
                        String.format("Detected text block %d has %d lines", i, lines.size)
                    )
                    for (j in lines.indices) {
                        val elements = lines[j].elements
                        Log.v(
                            MANUAL_TESTING_LOG,
                            String.format("Detected text line %d has %d elements", j, elements.size)
                        )
                        for (k in elements.indices) {
                            val element = elements[k]
                            Log.v(
                                MANUAL_TESTING_LOG,
                                String.format("Detected text element %d says: %s", k, element.text)
                            )
                            Log.v(
                                MANUAL_TESTING_LOG,
                                String.format(
                                    "Detected text element %d has a bounding box: %s",
                                    k,
                                    element.boundingBox!!.flattenToString()
                                )
                            )
                            Log.v(
                                MANUAL_TESTING_LOG,
                                String.format(
                                    "Expected corner point size is 4, get %d",
                                    element.cornerPoints!!.size
                                )
                            )
                            for (point in element.cornerPoints!!) {
                                Log.v(
                                    MANUAL_TESTING_LOG,
                                    String.format(
                                        "Corner point for element %d is located at: x - %d, y = %d",
                                        k,
                                        point.x,
                                        point.y
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

