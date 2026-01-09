package com.merryblue.baseapplication.ui.home.color

import com.merryblue.baseapplication.coredata.model.edge.EdgeColorItem

data class EdgeColorState(
    val selectedTab: EdgeTab = EdgeTab.TAB_4,
    val items: List<EdgeColorItem> = emptyList(),
    val selectedIndex: Int = -1
)

sealed interface EdgeColorIntent {
    data class SelectColor(val index: Int) : EdgeColorIntent
    data class SelectTab(val tab: EdgeTab) : EdgeColorIntent
}

enum class EdgeTab { TAB_4, TAB_3, TAB_2 }
