package com.merryblue.baseapplication.ui.home.advanced

import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.coredata.model.edge.EdgeAdvanced
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeHoleShape
import com.merryblue.baseapplication.ui.view.edgelight.model.InfinityShape

data class EdgeAdvancedState(
    val directionSelectedIndex: Int = -1,
    val notchTypeSelectedIndex: Int = -1,
    val listDirection: List<EdgeAdvanced.EdgeDirection> = emptyList(),
    val listNotchType: List<EdgeAdvanced.EdgeNotchType> = emptyList(),
    val currentNotchType: Advanced = Advanced.NOTCH_DEFAULT,
    val currentHoleShape: EdgeHoleShape = EdgeHoleShape.CIRCLE,
    val currentInfinityShape: InfinityShape = InfinityShape.U,
    val isLoaded: Boolean = false
)

sealed interface EdgeAdvancedIntent {
    data object LoadInitial : EdgeAdvancedIntent
    data class SelectDirection(val index: Int) : EdgeAdvancedIntent
    data class SelectNotchType(val index: Int) : EdgeAdvancedIntent

    // Notch adjustments
    data class UpdateNotchWidth(val fraction: Float) : EdgeAdvancedIntent
    data class UpdateNotchHeight(val heightPx: Float) : EdgeAdvancedIntent
    data class UpdateNotchTopRadius(val radiusPx: Float) : EdgeAdvancedIntent
    data class UpdateNotchBottomRadius(val radiusPx: Float) : EdgeAdvancedIntent
    data class UpdateNotchBottomFullness(val fullness: Float) : EdgeAdvancedIntent

    // Hole adjustments
    data class SelectHoleShape(val shape: EdgeHoleShape) : EdgeAdvancedIntent
    data class UpdateHoleOffsetX(val offset: Float) : EdgeAdvancedIntent
    data class UpdateHoleOffsetY(val offset: Float) : EdgeAdvancedIntent
    data class UpdateHoleRadius(val radius: Float) : EdgeAdvancedIntent
    data class UpdateHoleWidth(val widthPx: Float) : EdgeAdvancedIntent
    data class UpdateHoleHeight(val heightPx: Float) : EdgeAdvancedIntent
    data class UpdateHoleCornerRadius(val radiusPx: Float) : EdgeAdvancedIntent

    // Infinity adjustments
    data class SelectInfinityShape(val shape: InfinityShape) : EdgeAdvancedIntent
    data class UpdateInfinityWidth(val widthPx: Float) : EdgeAdvancedIntent
    data class UpdateInfinityHeight(val heightPx: Float) : EdgeAdvancedIntent
    data class UpdateInfinityTopRadius(val radiusPx: Float) : EdgeAdvancedIntent
}

sealed interface EdgeAdvancedEffect {
    data object RefreshLayout : EdgeAdvancedEffect
}