package com.merryblue.baseapplication.ui.picker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.helpers.dpToPx
import kotlin.math.max
import kotlin.math.min

class HueSliderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var cornerRadiusPx = 10f.dpToPx
    private var innerPaddingPx = 4f.dpToPx
    private var thumbRadiusPx = 8f.dpToPx
    private var thumbMarginPx = 0f.dpToPx
    private var clipToRoundRect = true
    private var useSoftwareLayer = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barRect = RectF()
    private val activeRect = RectF()
    private val clipPath = Path()

    private val thumbFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val thumbStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f.dpToPx
        color = Color.BLACK
        alpha = 120
    }

    private var shader: LinearGradient? = null

    private var hue = 0f // 0..359.999
    private var onHueChanged: ((Float) -> Unit)? = null
    private var onDraggingChanged: ((Boolean) -> Unit)? = null

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.HueSliderView)
            cornerRadiusPx = a.getDimension(R.styleable.HueSliderView_hueCornerRadius, cornerRadiusPx)
            innerPaddingPx = a.getDimension(R.styleable.HueSliderView_hueInnerPadding, innerPaddingPx)
            thumbRadiusPx = a.getDimension(R.styleable.HueSliderView_hueThumbRadius, thumbRadiusPx)
            thumbMarginPx = a.getDimension(R.styleable.HueSliderView_hueThumbMargin, thumbMarginPx)
            clipToRoundRect = a.getBoolean(R.styleable.HueSliderView_hueClipToRoundRect, true)
            useSoftwareLayer = a.getBoolean(R.styleable.HueSliderView_hueUseSoftwareLayer, false)
            a.recycle()
        }

        if (useSoftwareLayer) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
    }

    fun setHueChangedListener(cb: (Float) -> Unit) {
        onHueChanged = cb
    }

    fun setOnDraggingChangedListener(cb: (Boolean) -> Unit) {
        onDraggingChanged = cb
    }

    fun setHue(h: Float, notify: Boolean = true) {
        hue = clampHue(h)
        invalidate()
        if (notify) onHueChanged?.invoke(hue)
    }

    fun setFromColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        setHue(hsv[0], notify = true)
    }

    fun getHue(): Float = hue

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        barRect.set(
            paddingLeft + innerPaddingPx,
            paddingTop + innerPaddingPx,
            w - paddingRight - innerPaddingPx,
            h - paddingBottom - innerPaddingPx
        )

        val inset = thumbRadiusPx + thumbMarginPx
        activeRect.set(
            barRect.left,
            barRect.top + inset,
            barRect.right,
            barRect.bottom - inset
        )

        shader = LinearGradient(
            0f, barRect.top, 0f, barRect.bottom,
            intArrayOf(
                Color.RED, Color.YELLOW, Color.GREEN,
                Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
            ),
            null,
            Shader.TileMode.CLAMP
        )

        clipPath.reset()
        clipPath.addRoundRect(barRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.shader = shader

        if (clipToRoundRect) {
            val save = canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawRoundRect(barRect, cornerRadiusPx, cornerRadiusPx, paint)
            canvas.restoreToCount(save)
        } else {
            canvas.drawRoundRect(barRect, cornerRadiusPx, cornerRadiusPx, paint)
        }

        val y = activeRect.top + (hue / HUE_MAX) * activeRect.height()
        val x = barRect.centerX()

        canvas.drawCircle(x, y, thumbRadiusPx, thumbFill)
        canvas.drawCircle(x, y, thumbRadiusPx, thumbStroke)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onDraggingChanged?.invoke(true)
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onDraggingChanged?.invoke(false)
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (activeRect.height() <= 1f) return false
                val y = min(max(event.y, activeRect.top), activeRect.bottom)
                val t = ((y - activeRect.top) / activeRect.height()).coerceIn(0f, 1f)
                hue = t * HUE_MAX
                invalidate()
                onHueChanged?.invoke(hue)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun clampHue(h: Float): Float {
        var x = h % 360f
        if (x < 0f) x += 360f
        return x.coerceIn(0f, HUE_MAX)
    }

    companion object {
        private const val HUE_MAX = 359.999f
    }
}
