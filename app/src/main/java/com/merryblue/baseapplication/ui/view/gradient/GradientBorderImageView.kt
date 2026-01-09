package com.merryblue.baseapplication.ui.view.gradient

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.use
import com.merryblue.baseapplication.R

class GradientBorderImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private var strokeWidth = dp(3f)
    private var cornerRadius = dp(12f)
    private var colors = intArrayOf(Color.RED, Color.BLUE)

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val borderPath = Path()
    private val imagePath = Path()
    private val rect = RectF()

    private var gradient: SweepGradient? = null

    init {
        scaleType = ScaleType.CENTER_CROP
        attrs?.let { parseAttrs(it) }
        borderPaint.strokeWidth = strokeWidth
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
            if (cRes != 0) {
                colors = resources.getIntArray(cRes)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildBorder(w, h)
    }

    private fun buildBorder(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return

        val halfStroke = strokeWidth / 2f
        val radius = maxOf(0f, cornerRadius - halfStroke)

        rect.set(
            halfStroke,
            halfStroke,
            w - halfStroke,
            h - halfStroke
        )

        borderPath.reset()
        borderPath.addRoundRect(rect, radius, radius, Path.Direction.CW)

        imagePath.reset()
        imagePath.addRoundRect(
            RectF(
                strokeWidth,
                strokeWidth,
                w - strokeWidth,
                h - strokeWidth
            ),
            maxOf(0f, cornerRadius - strokeWidth),
            maxOf(0f, cornerRadius - strokeWidth),
            Path.Direction.CW
        )

        val cx = w / 2f
        val cy = h / 2f

        val sweepColors = if (colors.first() != colors.last()) {
            colors + colors.first()
        } else {
            colors
        }

        val positions = FloatArray(sweepColors.size) { i ->
            i.toFloat() / (sweepColors.size - 1)
        }

        gradient = SweepGradient(cx, cy, sweepColors, positions)

        val matrix = Matrix()
        matrix.postRotate(-90f, cx, cy)
        gradient!!.setLocalMatrix(matrix)

        borderPaint.shader = gradient
        borderPaint.strokeWidth = strokeWidth
    }

    override fun onDraw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipPath(imagePath)
        super.onDraw(canvas)
        canvas.restoreToCount(save)
        canvas.drawPath(borderPath, borderPaint)
    }

    fun setBorderColors(vararg c: Int) {
        if (c.size < 2) return
        colors = c
        buildBorder(width, height)
        invalidate()
    }

    fun setStrokeWidthDp(dp: Float) {
        strokeWidth = dp(dp)
        buildBorder(width, height)
        invalidate()
    }

    fun setCornerRadiusDp(dp: Float) {
        cornerRadius = dp(dp)
        buildBorder(width, height)
        invalidate()
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}