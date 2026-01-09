package com.merryblue.baseapplication.ui.home.effect

import com.merryblue.baseapplication.coredata.model.edge.EdgeEffectItem

data class EdgeEffectState(
    val selectedIndex: Int = -1,
    val listEffect: List<EdgeEffectItem> = emptyList(),
)