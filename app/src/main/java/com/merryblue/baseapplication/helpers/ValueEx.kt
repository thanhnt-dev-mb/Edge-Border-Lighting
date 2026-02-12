package com.merryblue.baseapplication.helpers

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.toColorInt

fun Float.mapFloatToRange(min: Float, max: Float): Float {
    val p = this.coerceIn(0f, 1f)
    return min + (max - min) * p
}

fun Float.mapFloatToRangeLong(min: Long, max: Long): Long {
    val p = this.coerceIn(0f, 1f)
    return (min + (max - min) * p).toLong()
}

fun Float.mapValueToProgress(min: Float, max: Float): Float {
    if (max == min) return 0f
    val v = this.coerceIn(min, max)
    return ((v - min) / (max - min)).coerceIn(0f, 1f)
}

fun Long.mapValueToProgress(min: Long, max: Long): Float {
    val minF = min.toFloat()
    val maxF = max.toFloat()
    if (maxF == minF) return 0f
    val v = this.toFloat().coerceIn(minF, maxF)
    return ((v - minF) / (maxF - minF)).coerceIn(0f, 1f)
}

val Int.dpToPx: Int get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

val Float.dpToPx: Float get() = this * android.content.res.Resources.getSystem().displayMetrics.density

val Int.toHex: String get() = String.format("#%08X", this)

val String.parseHexSafe: Int?
    get() {
        val t = trim().replace(" ", "")
        return try {
            when {
                t.startsWith("#") -> t.toColorInt()
                t.length == 6 -> "#$t".toColorInt()
                t.length == 8 -> "#$t".toColorInt()
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }