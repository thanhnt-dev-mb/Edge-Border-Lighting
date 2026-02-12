package com.merryblue.baseapplication.ui.home.color

import com.merryblue.baseapplication.coredata.model.edge.EdgeColorItem

data class EdgeColorState(
    val selectedTab: EdgeTab = EdgeTab.TAB_4,
    val items: List<EdgeColorItem> = emptyList(),
    val selectedIndex: Int = -1,
    val isLoaded: Boolean = false
)

sealed interface EdgeColorIntent {
    data object LoadInitial : EdgeColorIntent
    data class SelectColor(val index: Int) : EdgeColorIntent
    data class SelectTab(val tab: EdgeTab) : EdgeColorIntent
}

sealed interface EdgeColorEffect {
    data class ApplyColors(val colors: IntArray) : EdgeColorEffect
}

enum class EdgeTab { TAB_4, TAB_3, TAB_2 }
