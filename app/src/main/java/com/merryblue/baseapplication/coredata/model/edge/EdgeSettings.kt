package com.merryblue.baseapplication.coredata.model.edge

sealed class EdgeSettings {
    data class EdgeSpeed (
        val progress: Long
    ): EdgeSettings()

    data class EdgeSize(
        val progress: Float
    ): EdgeSettings()

    data class EdgeBottomRadius(
        val progress: Float
    ): EdgeSettings()

    data class EdgeTopRadius(
        val progress: Float
    ): EdgeSettings()
}