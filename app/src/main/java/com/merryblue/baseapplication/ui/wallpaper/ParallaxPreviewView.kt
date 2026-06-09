package com.merryblue.baseapplication.ui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.max

class ParallaxPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), SensorEventListener {

    private companion object {
        const val SENSOR_DELAY_US = 33_333
        const val LOW_PASS_ALPHA = 0.18f
        const val MAX_TILT_RADIANS = 1.0f
        const val MIN_TILT_DELTA = 0.0035f
        const val FALLBACK_WIDTH = 360
        const val FALLBACK_HEIGHT = 640
    }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val destinationRect = RectF()
    private val sensorManager by lazy(LazyThreadSafetyMode.NONE) {
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val motionSensor by lazy(LazyThreadSafetyMode.NONE) {
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)

    private var previewBitmap: Bitmap? = null
    private var ownedBitmap: Bitmap? = null
    private var backgroundColor = Color.BLACK
    private var attached = false
    private var windowVisible = true
    private var viewVisible = true
    private var motionEnabled = true
    private var sensorRegistered = false
    private var tiltX = 0f
    private var tiltY = 0f

    fun setPreviewBitmap(bitmap: Bitmap?) {
        replacePreviewBitmap(bitmap?.takeIf { !it.isRecycled }, ownsBitmap = false)
    }

    fun setPreviewDrawable(drawable: Drawable?) {
        if (drawable == null) {
            clearPreview()
            return
        }
        replacePreviewBitmap(drawable.toOwnedPreviewBitmap(), ownsBitmap = true)
    }

    fun clearPreview() {
        replacePreviewBitmap(bitmap = null, ownsBitmap = false)
    }

    fun setMotionEnabled(enabled: Boolean) {
        if (motionEnabled == enabled) return
        motionEnabled = enabled
        updateSensorState()
    }

    fun release() {
        unregisterSensorIfNeeded()
        replacePreviewBitmap(bitmap = null, ownsBitmap = false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        updateSensorState()
    }

    override fun onDetachedFromWindow() {
        attached = false
        updateSensorState()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        windowVisible = visibility == VISIBLE
        updateSensorState()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView === this) {
            viewVisible = visibility == VISIBLE
            updateSensorState()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(backgroundColor)
        val bitmap = previewBitmap?.takeIf { !it.isRecycled } ?: return
        if (width <= 0 || height <= 0 || bitmap.width <= 0 || bitmap.height <= 0) return

        val scale = max(width / bitmap.width.toFloat(), height / bitmap.height.toFloat()) *
            ParallaxWallpaperMotion.PREVIEW_ZOOM_FACTOR
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        val extraX = (scaledWidth - width).coerceAtLeast(0f)
        val extraY = (scaledHeight - height).coerceAtLeast(0f)
        val left = ((width - scaledWidth) * 0.5f) +
            (extraX * ParallaxWallpaperMotion.PREVIEW_MAX_TILT_SHIFT * tiltX)
        val top = ((height - scaledHeight) * 0.5f) +
            (extraY * ParallaxWallpaperMotion.PREVIEW_MAX_TILT_SHIFT * tiltY)

        destinationRect.set(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(bitmap, null, destinationRect, bitmapPaint)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!shouldRun()) return
        val next = when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationValues)
                Pair(
                    orientationValues[2].coerceIn(-MAX_TILT_RADIANS, MAX_TILT_RADIANS),
                    (-orientationValues[1]).coerceIn(-MAX_TILT_RADIANS, MAX_TILT_RADIANS)
                )
            }
            Sensor.TYPE_ACCELEROMETER -> {
                Pair(
                    (-event.values[0] / ParallaxWallpaperMotion.GRAVITY_EARTH).coerceIn(-1f, 1f),
                    (event.values[1] / ParallaxWallpaperMotion.GRAVITY_EARTH).coerceIn(-1f, 1f)
                )
            }
            else -> return
        }

        val nextX = tiltX + ((next.first - tiltX) * LOW_PASS_ALPHA)
        val nextY = tiltY + ((next.second - tiltY) * LOW_PASS_ALPHA)
        if (abs(nextX - tiltX) < MIN_TILT_DELTA && abs(nextY - tiltY) < MIN_TILT_DELTA) return
        tiltX = nextX
        tiltY = nextY
        postInvalidateOnAnimation()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun replacePreviewBitmap(bitmap: Bitmap?, ownsBitmap: Boolean) {
        val oldOwnedBitmap = ownedBitmap?.takeIf { it !== bitmap }
        ownedBitmap = bitmap.takeIf { ownsBitmap }
        previewBitmap = bitmap
        backgroundColor = bitmap?.takeIf { !it.isRecycled }?.averageCenterColor() ?: Color.BLACK
        oldOwnedBitmap?.safeRecycle()
        updateSensorState()
        postInvalidateOnAnimation()
    }

    private fun shouldRun(): Boolean {
        return attached &&
            windowVisible &&
            viewVisible &&
            motionEnabled &&
            previewBitmap?.isRecycled == false
    }

    private fun updateSensorState() {
        if (shouldRun()) registerSensorIfNeeded() else unregisterSensorIfNeeded()
    }

    private fun registerSensorIfNeeded() {
        if (sensorRegistered) return
        val sensor = motionSensor ?: return
        sensorRegistered = sensorManager.registerListener(this, sensor, SENSOR_DELAY_US)
    }

    private fun unregisterSensorIfNeeded() {
        if (!sensorRegistered) return
        sensorManager.unregisterListener(this)
        sensorRegistered = false
    }

    private fun Drawable.toOwnedPreviewBitmap(): Bitmap? {
        val targetWidth = width.takeIf { it > 0 }
            ?: intrinsicWidth.takeIf { it > 0 }
            ?: FALLBACK_WIDTH
        val targetHeight = height.takeIf { it > 0 }
            ?: intrinsicHeight.takeIf { it > 0 }
            ?: FALLBACK_HEIGHT
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val oldBounds = Rect(bounds)
        return runCatching {
            setBounds(0, 0, targetWidth, targetHeight)
            draw(canvas)
            setBounds(oldBounds)
            bitmap.prepareToDraw()
            bitmap
        }.getOrElse {
            setBounds(oldBounds)
            bitmap.safeRecycle()
            null
        }
    }

    private fun Bitmap.averageCenterColor(): Int {
        if (isRecycled || width <= 0 || height <= 0) return Color.BLACK
        return runCatching { getPixel(width / 2, height / 2) }.getOrDefault(Color.BLACK)
    }

    private fun Bitmap.safeRecycle() {
        if (!isRecycled) recycle()
    }
}
