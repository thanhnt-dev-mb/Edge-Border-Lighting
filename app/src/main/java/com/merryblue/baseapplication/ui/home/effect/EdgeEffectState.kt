package com.merryblue.baseapplication.ui.home.effect

import com.merryblue.baseapplication.coredata.model.edge.EdgeEffectItem

data class EdgeEffectState(
    val selectedIndex: Int = -1,
    val listEffect: List<EdgeEffectItem> = emptyList(),
    val isLoaded: Boolean = false
)

sealed interface EdgeEffectIntent {
    data object LoadInitial : EdgeEffectIntent
    data class SelectEffect(val index: Int) : EdgeEffectIntent
    data class UpdateSize(val sizePx: Float) : EdgeEffectIntent
    data class UpdateSpeed(val speedMs: Long) : EdgeEffectIntent
    data class UpdateBottomRadius(val radiusPx: Float) : EdgeEffectIntent
    data class UpdateTopRadius(val radiusPx: Float) : EdgeEffectIntent
}

sealed interface EdgeEffectEffect {
    // Reserved for future use if needed
}