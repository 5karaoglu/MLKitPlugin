package com.example.mlkitlib.ocr

import android.graphics.RectF

data class TextItem(var text: String, var lines: Int, var rect: RectF, var isSelected: Boolean)
