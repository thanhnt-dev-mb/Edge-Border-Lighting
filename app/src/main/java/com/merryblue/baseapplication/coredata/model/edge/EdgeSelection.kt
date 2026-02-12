package com.merryblue.baseapplication.coredata.model.edge

import com.merryblue.baseapplication.ui.home.color.EdgeTab

sealed class EdgeSelection {
    data class EdgeColor(
        val tab: EdgeTab,
        val selectedIndex: Int,
        val preset: EdgePreset
    ): EdgeSelection()

    data class EdgeEffect(
        val selectedIndex: Int,
        val preset: EdgePreset
    ): EdgeSelection()

    data class EdgeAdvanced(
        val selectedIndex: Int,
    ): EdgeSelection()
}

