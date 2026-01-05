package com.merryblue.baseapplication.helpers

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap

fun getSuitableConfig(
    image: Bitmap? = null
): Bitmap.Config = image?.config?.takeIf {
    it != Bitmap.Config.HARDWARE
} ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Bitmap.Config.RGBA_1010102
} else
    Bitmap.Config.RGBA_F16

fun Bitmap.toSoftware(): Bitmap = copy(getSuitableConfig(this), false) ?: this

val Bitmap.aspectRatio: Float get() = width / height.toFloat()

//val ImageBitmap.aspectRatio: Float get() = width / height.toFloat()

val Drawable.aspectRatio: Float get() = intrinsicWidth / intrinsicHeight.toFloat()

val Bitmap.safeAspectRatio: Float
    get() = aspectRatio
        .coerceAtLeast(0.005f)
        .coerceAtMost(1000f)

//val ImageBitmap.safeAspectRatio: Float
//    get() = aspectRatio
//        .coerceAtLeast(0.005f)
//        .coerceAtMost(1000f)

val Drawable.safeAspectRatio: Float
    get() = aspectRatio
        .coerceAtLeast(0.005f)
        .coerceAtMost(1000f)

fun Drawable.toBitmap(): Bitmap = toBitmap(config = getSuitableConfig())