package com.merryblue.baseapplication.ui.text.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import com.merryblue.baseapplication.helpers.dpToPx

class TextScrollerPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isSubpixelText = true
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
    }

    private var textValue: String = ""
    private var backgroundColor: Int = 0xFFFFFFFF.toInt()
    private var scrollerEnabled: Boolean = true
    private var blinkEnabled: Boolean = false
    private var blinkStartedAt: Long = SystemClock.uptimeMillis()
    private var lastScrollFrameAt: Long = 0L
    private var scrollOffsetPx: Float = 0f
    private var horizontalPaddingPx: Float = 8f.dpToPx
    private var scrollSpeedPxPerSecond: Float = 90f.dpToPx
    private var drawBackground: Boolean = true

    init {
        setTextSizeSp(60f)
        textPaint.color = 0xFF111827.toInt()
    }

    fun setDrawBackground(enabled: Boolean) {
        if (drawBackground == enabled) return
        drawBackground = enabled
        invalidate()
    }

    fun setTextValue(value: String) {
        if (textValue == value) return
        textValue = value
        normalizeScrollOffset()
        invalidate()
    }

    fun setTextSizeSp(sizeSp: Float) {
        textPaint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sizeSp,
            resources.displayMetrics
        )
        normalizeScrollOffset()
        invalidate()
    }

    fun setTextColorInt(@ColorInt color: Int) {
        if (textPaint.color == color) return
        textPaint.color = color
        invalidate()
    }

    fun setPreviewBackgroundColor(@ColorInt color: Int) {
        if (backgroundColor == color) return
        backgroundColor = color
        invalidate()
    }

    fun setPreviewTypeface(typeface: Typeface?) {
        val next = typeface ?: Typeface.DEFAULT_BOLD
        if (textPaint.typeface == next) return
        textPaint.typeface = next
        normalizeScrollOffset()
        invalidate()
    }

    fun setEffects(scroller: Boolean, blink: Boolean) {
        if (scrollerEnabled == scroller && blinkEnabled == blink) return
        if (scrollerEnabled != scroller) {
            lastScrollFrameAt = 0L
        }
        if (blinkEnabled != blink) {
            blinkStartedAt = SystemClock.uptimeMillis()
        }
        scrollerEnabled = scroller
        blinkEnabled = blink
        invalidate()
    }

    fun setScrollSpeedDpPerSecond(speedDp: Float) {
        scrollSpeedPxPerSecond = speedDp.dpToPx.coerceAtLeast(1f)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastScrollFrameAt = 0L
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawBackground) {
            canvas.drawColor(backgroundColor)
        }

        if (textValue.isBlank() || width <= 0 || height <= 0) return

        val now = SystemClock.uptimeMillis()
        textPaint.alpha = resolveTextAlpha(now)

        val baseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        val textWidth = textPaint.measureText(textValue).coerceAtLeast(1f)

        if (scrollerEnabled) {
            drawScrollingText(canvas, now, textWidth, baseline)
        } else {
            lastScrollFrameAt = 0L
            val x = ((width - textWidth) / 2f).coerceAtLeast(horizontalPaddingPx)
            canvas.drawText(textValue, x, baseline, textPaint)
        }

        if (scrollerEnabled || blinkEnabled) {
            postInvalidateOnAnimation()
        }
    }

    private fun resolveTextAlpha(now: Long): Int {
        if (!blinkEnabled) return 255
        return if (((now - blinkStartedAt) / BLINK_INTERVAL_MS) % 2L == 0L) 255 else 20
    }

    private fun drawScrollingText(canvas: Canvas, now: Long, textWidth: Float, baseline: Float) {
        val gap = 64f.dpToPx
        val repeatDistance = (textWidth + gap).coerceAtLeast(1f)
        val deltaMs = if (lastScrollFrameAt == 0L) 0L else (now - lastScrollFrameAt).coerceIn(0L, 64L)
        lastScrollFrameAt = now
        scrollOffsetPx = (scrollOffsetPx + deltaMs * scrollSpeedPxPerSecond / 1000f) % repeatDistance

        var x = width - scrollOffsetPx
        while (x > -textWidth) {
            canvas.drawText(textValue, x, baseline, textPaint)
            x -= repeatDistance
        }
    }

    private fun normalizeScrollOffset() {
        val repeatDistance = (textPaint.measureText(textValue).coerceAtLeast(1f) + 64f.dpToPx)
            .coerceAtLeast(1f)
        scrollOffsetPx %= repeatDistance
    }

    companion object {
        private const val BLINK_INTERVAL_MS = 450L
    }
}
