package com.merryblue.baseapplication.coredata.model

enum class CUE {
    ONE,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT
}

data class CueModel(
    val displayName: String,
    val cue: CUE,
    val timeDisplay: String
)
