package com.merryblue.baseapplication.ui.home.color

import com.merryblue.baseapplication.coredata.model.EdgeColorItem

data class EdgeColorState(
    val items: List<EdgeColorItem> = emptyList(),
    val selectedIndex: Int = -1
)