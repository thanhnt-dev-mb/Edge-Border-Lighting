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

class SvPickerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var cornerRadiusPx = 10f.dpToPx
    private var thumbRadiusPx = 6f.dpToPx
    private var thumbMarginPx = 2f.dpToPx
    private var clipToRoundRect = true
    private var useSoftwareLayer = true

    private val rect = RectF()
    private val activeRect = RectF()
    private val clipPath = Path()

    private val satPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val thumbFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.TRANSPARENT }
    private val thumbStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f.dpToPx
        color = Color.WHITE
    }

    private var hue = 0f
    private var sat = 1f
    private var value = 1f

    private var onColorChanged: ((Int) -> Unit)? = null
    private var onDraggingChanged: ((Boolean) -> Unit)? = null

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SvPickerView)
            cornerRadiusPx = a.getDimension(R.styleable.SvPickerView_svCornerRadius, cornerRadiusPx)
            thumbRadiusPx = a.getDimension(R.styleable.SvPickerView_svThumbRadius, thumbRadiusPx)
            thumbMarginPx = a.getDimension(R.styleable.SvPickerView_svThumbMargin, thumbMarginPx)
            clipToRoundRect = a.getBoolean(R.styleable.SvPickerView_svClipToRoundRect, true)
            useSoftwareLayer = a.getBoolean(R.styleable.SvPickerView_svUseSoftwareLayer, true)
            a.recycle()
        }

        if (useSoftwareLayer) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
    }

    fun setOnColorChangedListener(cb: (Int) -> Unit) {
        onColorChanged = cb
    }

    fun setOnDraggingChangedListener(cb: (Boolean) -> Unit) {
        onDraggingChanged = cb
    }

    fun setHue(h: Float) {
        hue = h
        buildShaders()
        invalidate()
        notifyColor()
    }

    fun setFromColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hue = hsv[0]
        sat = hsv[1]
        value = hsv[2]
        buildShaders()
        invalidate()
        notifyColor()
    }

    fun getColor(): Int = Color.HSVToColor(floatArrayOf(hue, sat, value))

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        rect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (w - paddingRight).toFloat(),
            (h - paddingBottom).toFloat()
        )

        val inset = thumbRadiusPx + thumbMarginPx
        activeRect.set(
            rect.left + inset,
            rect.top + inset,
            rect.right - inset,
            rect.bottom - inset
        )

        clipPath.reset()
        clipPath.addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)

        buildShaders()
        invalidate()
    }

    private fun buildShaders() {
        if (rect.width() <= 0f || rect.height() <= 0f) return

        val hueColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))

        satPaint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.top,
            Color.WHITE, hueColor, Shader.TileMode.CLAMP
        )

        valPaint.shader = LinearGradient(
            rect.left, rect.top, rect.left, rect.bottom,
            Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (clipToRoundRect) {
            val save = canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, satPaint)
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, valPaint)
            canvas.restoreToCount(save)
        } else {
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, satPaint)
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, valPaint)
        }

        val x = activeRect.left + sat * activeRect.width()
        val y = activeRect.top + (1f - value) * activeRect.height()

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
                if (activeRect.width() <= 1f || activeRect.height() <= 1f) return false

                val x = min(max(event.x, activeRect.left), activeRect.right)
                val y = min(max(event.y, activeRect.top), activeRect.bottom)

                sat = ((x - activeRect.left) / activeRect.width()).coerceIn(0f, 1f)
                value = (1f - (y - activeRect.top) / activeRect.height()).coerceIn(0f, 1f)

                invalidate()
                notifyColor()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun notifyColor() {
        onColorChanged?.invoke(getColor())
    }
}
