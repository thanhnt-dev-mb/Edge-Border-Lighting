package com.merryblue.baseapplication.ui.view.edgelight.model

import android.graphics.Bitmap

sealed class EdgeBackground {
    data class Color(val value: Int) : EdgeBackground()
    data class Image(val bitmap: Bitmap) : EdgeBackground()
}