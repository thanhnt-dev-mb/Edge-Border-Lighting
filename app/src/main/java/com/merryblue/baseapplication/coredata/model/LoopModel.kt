package com.merryblue.baseapplication.coredata.model

enum class LoopValue(val displayName: String) {
    EIGHT("8"),
    FOUR("4"),
    TWO("2"),
    ONE("1"),
    ONE_HALF("1/2"),
    ONE_QUARTER("1/4"),
    IN("IN"),
    OUT("OUT");

    fun getValueByType(): Float {
        return when (this) {
            EIGHT -> 8f
            FOUR -> 4f
            TWO -> 2f
            ONE -> 1f
            ONE_HALF -> 0.5f
            ONE_QUARTER -> 0.25f
            IN -> 0.125f
            OUT -> -1f
        }
    }
}

data class LoopModel(val loopValue: LoopValue)

fun LoopModel.findSmallerLooping(): LoopModel {
    val loopingValue =
        LoopValue.entries.find {
            val v = it.getValueByType()
            val current = loopValue.getValueByType()
            v < current }
    return if (loopingValue == null || loopingValue.getValueByType() in listOf(
            -1f
        )
    ) this else LoopModel(loopingValue)
}

fun LoopModel.findBiggerLooping(): LoopModel {
    val loopingValue =
        LoopValue.entries.findLast {
            val v = it.getValueByType()
            val current = loopValue.getValueByType()
            v > current }
    return if (loopingValue == null || loopingValue.getValueByType() in listOf(
            -1f
        )
    ) this else LoopModel(loopingValue)
}