package com.merryblue.baseapplication.coredata.model.edge

sealed class DisplayNotchType {
    data class TypeDisplayNotch(
        val progress: Float,
        val type: DisplayNotch
    ): DisplayNotchType()

    data class TypeDisplayHole(
        val progress: Float,
        val type: DisplayHole
    ): DisplayNotchType()

    data class TypeDisplayInfinity(
        val progress: Float,
        val type: DisplayInfinity
    ): DisplayNotchType()
}

enum class HoleType {
    CIRCLE, ROUND
}

enum class InfinityType {
    INFINITY_U,
    INFINITY_V
}