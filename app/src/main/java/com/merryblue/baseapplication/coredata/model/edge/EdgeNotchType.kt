package com.merryblue.baseapplication.coredata.model.edge

sealed class DisplayNotchType {

    open val progress: Float = 0f

    data class TypeDisplayNotch(
        override val progress: Float,
        val type: DisplayNotch
    ): DisplayNotchType()

    data class TypeDisplayHole(
        override val progress: Float,
        val type: DisplayHole
    ): DisplayNotchType()

    data class TypeDisplayInfinity(
        override val progress: Float,
        val type: DisplayInfinity
    ): DisplayNotchType()
}