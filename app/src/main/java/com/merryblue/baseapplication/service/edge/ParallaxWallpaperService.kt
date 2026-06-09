package com.merryblue.baseapplication.service.edge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_PARALLAX_WALLPAPER_CHANGED
import com.merryblue.baseapplication.ui.wallpaper.ParallaxWallpaperMotion
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class ParallaxWallpaperService : WallpaperService() {

    companion object {
        private const val MIN_FRAME_DELAY_MS = 16L
        private const val MIN_TILT_DELTA = 0.01f
        private const val MIN_OFFSET_DELTA = 0.0025f
    }

    private val prefs by lazy { AppPreferences(this) }

    override fun onCreateEngine(): Engine = ParallaxEngine()

    inner class ParallaxEngine : Engine(), SensorEventListener {

        private val sensorManager by lazy(LazyThreadSafetyMode.NONE) {
            getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
        private val motionSensor by lazy(LazyThreadSafetyMode.NONE) {
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        private val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        private val drawRect = RectF()
        private val drawThread = HandlerThread("ParallaxWallpaperDraw")
        private lateinit var drawHandler: Handler
        private val drawScheduled = AtomicBoolean(false)

        @Volatile private var currentHolder: SurfaceHolder? = null
        @Volatile private var visible = false
        private var isReceiverRegistered = false
        private var isSensorRegistered = false

        private var bitmap: Bitmap? = null
        private var bitmapPath: String? = null
        private var renderBitmap: Bitmap? = null
        private var bitmapReloadPending = true
        private var bitmapReloadForce = true

        private var xOffsetRatio = 0.5f
        private val rotationMatrix = FloatArray(9)
        private val orientationValues = FloatArray(3)
        private var tiltX = 0f
        private var tiltY = 0f
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var cachedDrawWidth = 0f
        private var cachedDrawHeight = 0f
        private var cachedExtraX = 0f
        private var cachedExtraY = 0f
        private var lastDrawUptime = 0L

        private val refreshReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != ACTION_PARALLAX_WALLPAPER_CHANGED) return
                requestBitmapReload(force = true)
                scheduleDraw(force = true)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            drawThread.start()
            drawHandler = Handler(drawThread.looper)
            setOffsetNotificationsEnabled(true)
            registerRefreshReceiverIfNeeded()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            currentHolder = holder
            requestBitmapReload(force = false)
            scheduleDraw(force = true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            currentHolder = holder
            surfaceWidth = width
            surfaceHeight = height
            requestBitmapReload(force = true)
            scheduleDraw(force = true)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            if (currentHolder === holder) currentHolder = null
            drawHandler.removeCallbacksAndMessages(null)
            drawScheduled.set(false)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            this.visible = visible
            if (visible) {
                registerSensorIfNeeded()
                requestBitmapReload(force = false)
                scheduleDraw(force = true)
            } else {
                unregisterSensorIfNeeded()
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            val nextOffsetRatio = if (xOffsetStep > 0f) xOffset.coerceIn(0f, 1f) else 0.5f
            if (abs(nextOffsetRatio - xOffsetRatio) < MIN_OFFSET_DELTA) return
            xOffsetRatio = nextOffsetRatio
            scheduleDraw()
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (!visible) return
            val nextTilt = when (event.sensor.type) {
                Sensor.TYPE_GAME_ROTATION_VECTOR,
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationValues)
                    Pair(
                        orientationValues[2].coerceIn(-1f, 1f),
                        (-orientationValues[1]).coerceIn(-1f, 1f)
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

            val nextTiltX = nextTilt.first
            val nextTiltY = nextTilt.second
            if (abs(nextTiltX - tiltX) < MIN_TILT_DELTA && abs(nextTiltY - tiltY) < MIN_TILT_DELTA) return

            tiltX += (nextTiltX - tiltX) * ParallaxWallpaperMotion.TILT_SMOOTHING
            tiltY += (nextTiltY - tiltY) * ParallaxWallpaperMotion.TILT_SMOOTHING
            scheduleDraw()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

        override fun onDestroy() {
            unregisterSensorIfNeeded()
            unregisterRefreshReceiverIfNeeded()
            recycleBitmap()
            if (::drawHandler.isInitialized) drawHandler.removeCallbacksAndMessages(null)
            drawThread.quitSafely()
            super.onDestroy()
        }

        private fun registerRefreshReceiverIfNeeded() {
            if (isReceiverRegistered) return
            val filter = IntentFilter(ACTION_PARALLAX_WALLPAPER_CHANGED)
            ContextCompat.registerReceiver(
                this@ParallaxWallpaperService,
                refreshReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
        }

        private fun unregisterRefreshReceiverIfNeeded() {
            if (!isReceiverRegistered) return
            try {
                this@ParallaxWallpaperService.unregisterReceiver(refreshReceiver)
            } catch (_: Throwable) {
            }
            isReceiverRegistered = false
        }

        private fun registerSensorIfNeeded() {
            if (isSensorRegistered) return
            val sensor = motionSensor ?: return
            isSensorRegistered = sensorManager.registerListener(this, sensor, 16_666)
        }

        private fun unregisterSensorIfNeeded() {
            if (!isSensorRegistered) return
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
        }

        private fun requestBitmapReload(force: Boolean) {
            bitmapReloadPending = true
            if (force) bitmapReloadForce = true
        }

        private fun reloadBitmapIfNeeded() {
            if (!bitmapReloadPending) return
            reloadBitmap(force = bitmapReloadForce)
            bitmapReloadPending = false
            bitmapReloadForce = false
        }

        private fun reloadBitmap(force: Boolean) {
            val path = prefs.parallaxWallpaperPath?.takeIf { it.isNotBlank() }
            if (path == null) {
                recycleBitmap()
                return
            }
            if (!force && path == bitmapPath && bitmap != null) return

            val decoded = decodeBitmap(path)
            recycleBitmap()
            bitmap = decoded
            bitmapPath = path.takeIf { decoded != null }
            updateRenderMetrics(decoded)
        }

        private fun decodeBitmap(path: String): Bitmap? {
            val targetWidth = surfaceWidth.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
            val targetHeight = surfaceHeight.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
            val requiredWidth = (targetWidth * ParallaxWallpaperMotion.ZOOM_FACTOR).roundToInt().coerceAtLeast(1)
            val requiredHeight = (targetHeight * ParallaxWallpaperMotion.ZOOM_FACTOR).roundToInt().coerceAtLeast(1)

            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, bounds)

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, requiredWidth, requiredHeight)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            return BitmapFactory.decodeFile(path, options)?.also { it.prepareToDraw() }
        }

        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                var halfHeight = height / 2
                var halfWidth = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                    halfHeight /= 2
                    halfWidth /= 2
                }
            }
            return inSampleSize.coerceAtLeast(1)
        }

        private fun updateRenderMetrics(source: Bitmap?) {
            val bitmap = source ?: run {
                cachedDrawWidth = 0f
                cachedDrawHeight = 0f
                cachedExtraX = 0f
                cachedExtraY = 0f
                recycleRenderBitmap()
                return
            }
            val targetWidth = surfaceWidth.takeIf { it > 0 }?.toFloat() ?: return
            val targetHeight = surfaceHeight.takeIf { it > 0 }?.toFloat() ?: return
            val scale = max(targetWidth / bitmap.width, targetHeight / bitmap.height) *
                ParallaxWallpaperMotion.ZOOM_FACTOR
            cachedDrawWidth = bitmap.width * scale
            cachedDrawHeight = bitmap.height * scale
            cachedExtraX = (cachedDrawWidth - targetWidth).coerceAtLeast(0f)
            cachedExtraY = (cachedDrawHeight - targetHeight).coerceAtLeast(0f)
            rebuildRenderBitmap(bitmap)
        }

        private fun rebuildRenderBitmap(source: Bitmap) {
            val targetWidth = cachedDrawWidth.roundToInt().coerceAtLeast(1)
            val targetHeight = cachedDrawHeight.roundToInt().coerceAtLeast(1)
            val currentRender = renderBitmap
            if (
                currentRender != null &&
                !currentRender.isRecycled &&
                currentRender.width == targetWidth &&
                currentRender.height == targetHeight
            ) {
                return
            }

            recycleRenderBitmap()
            renderBitmap = if (source.width == targetWidth && source.height == targetHeight) {
                source
            } else {
                Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true).also { it.prepareToDraw() }
            }
        }

        private fun recycleBitmap() {
            recycleRenderBitmap()
            bitmap?.takeIf { !it.isRecycled }?.recycle()
            bitmap = null
            bitmapPath = null
            cachedDrawWidth = 0f
            cachedDrawHeight = 0f
            cachedExtraX = 0f
            cachedExtraY = 0f
        }

        private fun recycleRenderBitmap() {
            renderBitmap?.takeIf { it !== bitmap && !it.isRecycled }?.recycle()
            renderBitmap = null
        }

        private fun scheduleDraw(force: Boolean = false) {
            if (!drawScheduled.compareAndSet(false, true)) return
            val now = SystemClock.uptimeMillis()
            val delayMs = if (force) 0L else (lastDrawUptime + MIN_FRAME_DELAY_MS - now).coerceAtLeast(0L)
            drawHandler.postDelayed({
                drawScheduled.set(false)
                drawFrame()
            }, delayMs)
        }

        private fun drawFrame() {
            val holder = currentHolder ?: return
            if (!holder.surface.isValid) return

            reloadBitmapIfNeeded()
            val source = bitmap ?: return
            val preparedBitmap = renderBitmap ?: source

            val canvas = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
            } catch (_: Throwable) {
                null
            } ?: return

            try {
                renderCanvas(canvas, source, preparedBitmap)
                lastDrawUptime = SystemClock.uptimeMillis()
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: Throwable) {
                }
            }
        }

        private fun renderCanvas(canvas: Canvas, source: Bitmap, preparedBitmap: Bitmap) {
            canvas.drawColor(Color.BLACK)

            if (cachedDrawWidth <= 0f || cachedDrawHeight <= 0f) {
                updateRenderMetrics(source)
            }

            val baseLeft = -cachedExtraX * xOffsetRatio
            val baseTop = -cachedExtraY * 0.5f
            val left = (baseLeft + (cachedExtraX * ParallaxWallpaperMotion.MAX_TILT_SHIFT * tiltX))
                .coerceIn(-cachedExtraX, 0f)
            val top = (baseTop + (cachedExtraY * ParallaxWallpaperMotion.MAX_TILT_SHIFT * tiltY))
                .coerceIn(-cachedExtraY, 0f)

            if (preparedBitmap === source) {
                drawRect.set(left, top, left + cachedDrawWidth, top + cachedDrawHeight)
                canvas.drawBitmap(source, null, drawRect, drawPaint)
            } else {
                canvas.drawBitmap(preparedBitmap, left, top, drawPaint)
            }
        }
    }
}
