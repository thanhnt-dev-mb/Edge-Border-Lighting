package com.merryblue.baseapplication.ui.view.gradient

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.use
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.helpers.dpToPx
import kotlin.math.max

class GradientBorderImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private var strokeWidth = 3f.dpToPx
    private var cornerRadius = 12f.dpToPx
    private var colors = intArrayOf(Color.RED, Color.BLUE)

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isDither = true
    }

    private val borderPath = Path()
    private val borderRect = RectF()
    private val shaderMatrix = Matrix()

    // Cache border bitmap
    private var borderBitmap: Bitmap? = null
    private var borderCanvas: Canvas? = null

    private var needRebuild = true

    init {
        scaleType = ScaleType.CENTER_CROP
        attrs?.let { parseAttrs(it) }

        borderPaint.strokeWidth = strokeWidth

        // Fast clip for image content (avoid clipPath cost)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val left = strokeWidth.toInt()
                    val top = strokeWidth.toInt()
                    val right = (view.width - strokeWidth).toInt()
                    val bottom = (view.height - strokeWidth).toInt()
                    val r = max(0f, cornerRadius - strokeWidth)
                    if (right > left && bottom > top) {
                        outline.setRoundRect(left, top, right, bottom, r)
                    }
                }
            }
            clipToOutline = true
        }
    }

    private fun parseAttrs(attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.GradientBorderImageView).use {
            strokeWidth = it.getDimension(
                R.styleable.GradientBorderImageView_gbStrokeWidth,
                strokeWidth
            )
            cornerRadius = it.getDimension(
                R.styleable.GradientBorderImageView_gbCornerRadius,
                cornerRadius
            )
            val cRes = it.getResourceId(
                R.styleable.GradientBorderImageView_gbColors,
                0
            )
            if (cRes != 0) colors = resources.getIntArray(cRes)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releaseBorderCache()
    }

    private fun releaseBorderCache() {
        borderCanvas = null
        borderBitmap?.recycle()
        borderBitmap = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            releaseBorderCache()
            needRebuild = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) invalidateOutline()
        }
    }

    private fun ensureBorderCache() {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        if (!needRebuild && borderBitmap != null) return

        // Create cache bitmap if needed
        if (borderBitmap == null) {
            borderBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            borderCanvas = Canvas(borderBitmap!!)
        }

        // Clear bitmap
        borderBitmap!!.eraseColor(Color.TRANSPARENT)

        val halfStroke = strokeWidth / 2f
        val radius = max(0f, cornerRadius - halfStroke)

        borderRect.set(halfStroke, halfStroke, w - halfStroke, h - halfStroke)

        borderPath.reset()
        borderPath.addRoundRect(borderRect, radius, radius, Path.Direction.CW)

        // Build SweepGradient (seamless)
        val cx = w / 2f
        val cy = h / 2f

        val first = colors.firstOrNull() ?: Color.TRANSPARENT
        val last = colors.lastOrNull() ?: first

        val sweepColors = if (colors.size >= 2 && first != last) {
            IntArray(colors.size + 1).also { out ->
                System.arraycopy(colors, 0, out, 0, colors.size)
                out[out.lastIndex] = first
            }
        } else {
            colors
        }

        val n = sweepColors.size
        val positions = FloatArray(n)
        val denom = max(1, n - 1)
        for (i in 0 until n) {
            positions[i] = i.toFloat() / denom
        }

        val gradient = SweepGradient(cx, cy, sweepColors, positions)
        shaderMatrix.reset()
        shaderMatrix.postRotate(-90f, cx, cy)
        gradient.setLocalMatrix(shaderMatrix)

        borderPaint.shader = gradient
        borderPaint.strokeWidth = strokeWidth

        borderCanvas!!.drawPath(borderPath, borderPaint)

        needRebuild = false
    }

    override fun onDraw(canvas: Canvas) {
        // Image draw (already clipped by outline on L+)
        super.onDraw(canvas)

        // Border draw from cache bitmap (fast)
        ensureBorderCache()
        borderBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    fun setBorderColors(vararg c: Int) {
        if (c.size < 2) return
        colors = c
        needRebuild = true
        invalidate()
    }

    fun setStrokeWidthDp(dp: Float) {
        strokeWidth = dp.dpToPx
        borderPaint.strokeWidth = strokeWidth
        needRebuild = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) invalidateOutline()
        invalidate()
    }

    fun setCornerRadiusDp(dp: Float) {
        cornerRadius = dp.dpToPx
        needRebuild = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) invalidateOutline()
        invalidate()
    }
}
