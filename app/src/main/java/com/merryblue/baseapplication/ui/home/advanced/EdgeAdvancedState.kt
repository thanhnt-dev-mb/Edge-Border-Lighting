package com.merryblue.baseapplication.ui.home.advanced

import com.merryblue.baseapplication.coredata.model.edge.EdgeAdvanced

data class EdgeAdvancedState(
    val directionSelectedIndex: Int = -1,
    val notchTypeSelectedIndex: Int = -1,
    val listDirection: List<EdgeAdvanced.EdgeDirection> = emptyList(),
    val listNotchType: List<EdgeAdvanced.EdgeNotchType> = emptyList(),
)