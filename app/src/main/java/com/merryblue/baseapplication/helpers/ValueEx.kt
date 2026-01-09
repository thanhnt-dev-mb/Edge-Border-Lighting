package com.merryblue.baseapplication.helpers

fun Float.mapFloatToRange(min: Float, max: Float): Float {
    val p = this.coerceIn(0f, 1f)
    return min + (max - min) * p
}

fun Float.mapFloatToRangeLong(min: Long, max: Long): Long {
    val p = this.coerceIn(0f, 1f)
    return (min + (max - min) * p).toLong()
}