package com.merryblue.baseapplication.coredata.model.edge

import androidx.annotation.DrawableRes
import com.merryblue.baseapplication.helpers.dpToPx

sealed class EdgeStyle {
    data class LinearColor(
        val colors: IntArray
    ): EdgeStyle()

    data class Pattern(
        @DrawableRes val vectorResId: Int,
        val iconSizePx: Float = 14f.dpToPx,
        val advancePx: Float = 26f.dpToPx,
        val rotate: Boolean = true,
        val phaseMultiplier: Float = 1f,
        val evenSpacing: Boolean = true
    ): EdgeStyle()

    object None : EdgeStyle()
}