package com.merryblue.baseapplication.ui.home

import android.graphics.Color
import android.net.Uri
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.helpers.BackgroundType.BACKGROUND_COLOR
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.ui.view.edgelight.EdgeHoleShape
import com.merryblue.baseapplication.ui.view.edgelight.InfinityShape

data class EdgeLightingState(

    // color
    val colors: IntArray = intArrayOf(
        Color.CYAN,
        Color.MAGENTA,
        Color.YELLOW,
        Color.CYAN
    ),

    // effect
    val vectorResId: Int = R.drawable.ic_none,
    val iconSizePx: Float = 8f.dpToPx,
    val advancePx: Float = 18f.dpToPx,
    val rotate: Boolean = true,
    val phaseMultiplier: Float = 0.1f,
    val speedMs: Long = 2500L,

    val topRadius: Float = 24f.dpToPx,
    val bottomRadius: Float = 24f.dpToPx,

    // advanced
    val direction: Advanced = Advanced.DIRECTION_CLOCKWISE,
    val notchType: Advanced = Advanced.NOTCH_DEFAULT,

    // display notch
    val notchWidthFraction: Float = 0.35677505f,
    val notchHeightPx: Float = 73.51973f.dpToPx,
    val notchTopRadiusPx: Float = 39.304764f.dpToPx,
    val notchBottomRadiusPx: Float = 29.377974f.dpToPx,
    val notchBottomFullness: Float = 0f,

    // display hole
    val holeOffsetX: Float = 0f,
    val holeOffsetY: Float = 40f.dpToPx,
    val holeRadius: Float = 14f.dpToPx,
    val holeWidthPx: Float = 64f.dpToPx,
    val holeHeightPx: Float = 28f.dpToPx,
    val holeCornerRadiusPx: Float = 14f.dpToPx,
    val holeShape: EdgeHoleShape = EdgeHoleShape.CIRCLE,

    // display infinity
    val infinityWidthPx: Float = -1f,
    val infinityHeightPx: Float = 20f.dpToPx,
    val infinityRadiusTopPx: Float = -1f,
    val infinityShape: InfinityShape = InfinityShape.U,

    val edgeStyleType: Int = 0,       // 0 linear, 1 pattern, 2 none
    val patternEnabled: Boolean = false,

    val backgroundType: Int = BACKGROUND_COLOR,      // 0 color, 1 resId, 2 url, 3 uri
    val backgroundColor: Int = Color.TRANSPARENT,
    val backgroundImageResId: Int = 0,
    val backgroundImageUrl: String? = null,
    val backgroundImageUri: Uri? = null,

    val isEnableEdgeLighting: Boolean = false
)