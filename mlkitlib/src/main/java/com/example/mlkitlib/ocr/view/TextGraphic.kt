/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mlkitlib.ocr.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.example.mlkitlib.ocr.TextItem
import kotlin.math.max
import kotlin.math.min

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
class TextGraphic
constructor(
  private val overlay: GraphicOverlay?,
  private val text: List<TextItem>,
  private val shouldGroupTextInBlocks: Boolean,
  private val showLanguageTag: Boolean,
  private val showConfidence: Boolean
) : GraphicOverlay.Graphic(overlay) {

  private val rectPaint: Paint = Paint()
  private val selectedRectPaint: Paint = Paint()
  private val textPaint: Paint
  private val labelPaint: Paint
  private val rectCornerRadius = 15F

  init {
    rectPaint.color = MARKER_COLOR
    rectPaint.style = Paint.Style.STROKE
    rectPaint.strokeWidth = STROKE_WIDTH
    selectedRectPaint.color = SELECTED_MARKER_COLOR
    selectedRectPaint.style = Paint.Style.STROKE
    selectedRectPaint.strokeWidth = STROKE_WIDTH
    textPaint = Paint()
    textPaint.color = TEXT_COLOR
    textPaint.textSize = TEXT_SIZE
    labelPaint = Paint()
    labelPaint.color = MARKER_COLOR
    labelPaint.style = Paint.Style.FILL
    // Redraw the overlay, as this graphic has been added.
    postInvalidate()

  }


  /** Draws the text block annotations for position, size, and raw value on the supplied canvas. */
  override fun draw(canvas: Canvas) {
    for (textBlock in text) { // Renders the text at the bottom of the box.
      /*Log.d(TAG, "TextBlock text is: " + textBlock.text)
      Log.d(TAG, "TextBlock boundingbox is: " + textBlock.boundingBox)
      Log.d(TAG, "TextBlock cornerpoint is: " + Arrays.toString(textBlock.cornerPoints))*/
      if (shouldGroupTextInBlocks) {
        drawText(
          getFormattedText(textBlock.text, "", confidence = null),
          textBlock.isSelected,
          textBlock.rect,
          TEXT_SIZE * textBlock.lines + 2 * STROKE_WIDTH,
          canvas
        )
      }
    }
  }

  private fun getFormattedText(text: String, languageTag: String, confidence: Float?): String {
    val res = if (showLanguageTag) String.format(TEXT_WITH_LANGUAGE_TAG_FORMAT, languageTag, text) else text
    return if (showConfidence && confidence != null) String.format("%s (%.2f)", res, confidence)
    else res
  }

  private fun drawText(text: String, isSelected: Boolean, rect: RectF, textHeight: Float, canvas: Canvas) {
    Log.d(TAG, "drawText: ${rect.left},${rect.right},${rect.top},${rect.bottom}")

    if (isSelected) canvas.drawRoundRect(rect, rectCornerRadius,rectCornerRadius,selectedRectPaint)
    else canvas.drawRoundRect(rect, rectCornerRadius,rectCornerRadius,rectPaint)

    Log.v("textLogg", text)
  }


  companion object {
    private const val TAG = "TextGraphic"
    private const val TEXT_WITH_LANGUAGE_TAG_FORMAT = "%s:%s"
    private const val TEXT_COLOR = Color.MAGENTA
    private const val MARKER_COLOR = Color.RED
    private const val SELECTED_MARKER_COLOR = Color.GREEN
    private const val TEXT_SIZE = 46.0f
    private const val STROKE_WIDTH = 4.0f
  }
}
