package com.merryblue.baseapplication.ui.view.ripple

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class RipplePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val waves = ArrayList<Wave>(32)

    private var bgBitmap: Bitmap? = null
    private var bgShader: BitmapShader? = null
    private val bgMatrix = Matrix()

    private val waveDurationMs = 900L
    private val maxRadiusFactor = 0.55f
    private val maxWavesOnScreen = 10

    fun setBackgroundBitmap(bitmap: Bitmap?) {
        bgBitmap = bitmap
        updateShader()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateShader()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> addWave(event.x, event.y)
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // bg
        val shader = bgShader
        if (shader == null) {
            canvas.drawColor(Color.rgb(8, 15, 22))
        } else {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
        }

        // waves (update + draw)
        val now = SystemClock.elapsedRealtime()
        val maxR = min(width, height) * maxRadiusFactor

        var i = 0
        while (i < waves.size) {
            val t = now - waves[i].start
            if (t >= waveDurationMs) waves.removeAt(i) else i++
        }

        for (wave in waves) {
            val t = ((now - wave.start).toFloat() / waveDurationMs).coerceIn(0f, 1f)
            val radius = lerp(0f, maxR, easeOutCubic(t))
            val alpha = (255 * (1f - t)).toInt().coerceIn(0, 255)

            paint.color = Color.argb(alpha, 200, 230, 255)
            paint.strokeWidth = 6f
            canvas.drawCircle(wave.x, wave.y, radius, paint)

            paint.color = Color.argb((alpha * 0.6f).toInt(), 180, 210, 255)
            paint.strokeWidth = 3f
            canvas.drawCircle(wave.x, wave.y, radius * 0.72f, paint)
        }

        // loop invalidate để animate
        if (waves.isNotEmpty()) postInvalidateOnAnimation()
    }

    private fun addWave(x: Float, y: Float) {
        val now = SystemClock.elapsedRealtime()
        if (waves.size >= maxWavesOnScreen) waves.removeAt(0)
        waves.add(Wave(x, y, now))
        postInvalidateOnAnimation()
    }

    private fun updateShader() {
        val bmp = bgBitmap ?: run { bgShader = null; return }
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        val scale = max(w / bw, h / bh)
        val dx = (w - bw * scale) * 0.5f
        val dy = (h - bh * scale) * 0.5f

        bgMatrix.reset()
        bgMatrix.postScale(scale, scale)
        bgMatrix.postTranslate(dx, dy)

        val shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(bgMatrix)
        bgShader = shader
    }

    private data class Wave(val x: Float, val y: Float, val start: Long)

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
    private fun easeOutCubic(t: Float): Float {
        val inv = 1f - t
        return 1f - inv * inv * inv
    }
}