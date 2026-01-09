package com.merryblue.baseapplication.coredata.model.edge

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class EdgeAdvanced {
    data class EdgeDirection(
        val type: Advanced,
        @StringRes val title: Int,
        @DrawableRes val resId: Int,
        var isSelected: Boolean,
    ) : EdgeAdvanced()

    data class EdgeNotchType(
        val type: Advanced,
        @StringRes val title: Int,
        @DrawableRes val resId: Int,
        var isSelected: Boolean,
    ) : EdgeAdvanced()
}

enum class Advanced {
    DIRECTION_CLOCKWISE,
    DIRECTION_ANTI_CLOCKWISE,
    DIRECTION_TOP_RIGHT_BOTTOM_LEFT,
    DIRECTION_BOTTOM_LEFT_TOP_RIGHT,
    DIRECTION_DOWN,
    DIRECTION_UP,
    NOTCH_DEFAULT,
    NOTCH_DISPLAY_NOTCH,
    NOTCH_DISPLAY_HOLE,
    NOTCH_DISPLAY_INFINITY
}