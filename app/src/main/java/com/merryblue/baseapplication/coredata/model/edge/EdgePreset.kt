package com.merryblue.baseapplication.coredata.model.edge

import androidx.annotation.DrawableRes

sealed class EdgePreset {

    data class BackgroundColor(
        val color: Int,
        val edge: EdgeStyle,
    ) : EdgePreset()

    data class BackgroundImageRes(
        @DrawableRes val resId: Int,
        val edge: EdgeStyle
    ) : EdgePreset()
}