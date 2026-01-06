package com.merryblue.baseapplication.ui.view.edgelight

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.res.use
import com.merryblue.baseapplication.R

class EdgeLightingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var direction = EdgeDirection.CLOCKWISE
    private var notchType = NotchType.DEFAULT
    private var imageScaleType = EdgeImageScaleType.CENTER_CROP

    private var strokeWidth = dp(6f)
    private var cornerRadius = dp(24f)
    private var duration = 2500L

    private var holeRadius = dp(14f)
    private var holeOffsetX = 0f
    private var holeOffsetY = dp(10f)
    private var infinityCurveDepth = dp(20f)
    private var backgroundInside: EdgeBackground? = null

    private var colors = intArrayOf(
        Color.CYAN,
        Color.MAGENTA,
        Color.YELLOW,
        Color.CYAN
    )

    private var animationEnabled = true
    private var progress = 0f

    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = this@EdgeLightingView.strokeWidth
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    private var isViewAttached = false

    private var cachedPath: Path? = null
    private var cachedWidth = 0
    private var cachedHeight = 0

    private var cachedShader: Shader? = null
    private var cachedShaderColors: IntArray? = null
    private val animationMatrix = Matrix()

    private var cachedBitmapShader: BitmapShader? = null
    private var cachedBitmap: Bitmap? = null
    private val bgMatrix = Matrix()

    init {
        attrs?.let { parseAttrs(it) }
        edgePaint.strokeWidth = strokeWidth
        animator.duration = duration
        if (animationEnabled && isViewAttached) animator.start()
    }

    private fun parseAttrs(attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.EdgeLightingView).use {
            direction = when (it.getInt(R.styleable.EdgeLightingView_edgeDirection, 0)) {
                1 -> EdgeDirection.ANTI_CLOCKWISE
                2 -> EdgeDirection.TOP_RIGHT_TO_BOTTOM_LEFT
                3 -> EdgeDirection.BOTTOM_LEFT_TO_TOP_RIGHT
                4 -> EdgeDirection.TOP_TO_BOTTOM
                5 -> EdgeDirection.BOTTOM_TO_TOP
                else -> EdgeDirection.CLOCKWISE
            }

            notchType = when (it.getInt(R.styleable.EdgeLightingView_edgeNotchType, 0)) {
                1 -> NotchType.DISPLAY_NOTCH
                2 -> NotchType.DISPLAY_HOLE
                3 -> NotchType.DISPLAY_INFINITY
                else -> NotchType.DEFAULT
            }

            imageScaleType = when (it.getInt(R.styleable.EdgeLightingView_edgeImageScaleType, 1)) {
                0 -> EdgeImageScaleType.FIT_XY
                else -> EdgeImageScaleType.CENTER_CROP
            }

            animationEnabled = it.getBoolean(R.styleable.EdgeLightingView_edgeAnimationEnabled,true)
            strokeWidth = it.getDimension(R.styleable.EdgeLightingView_edgeStrokeWidth, strokeWidth)
            cornerRadius = it.getDimension(R.styleable.EdgeLightingView_edgeCornerRadius, cornerRadius)
            duration = it.getInt(R.styleable.EdgeLightingView_edgeDuration, duration.toInt()).toLong()
            holeRadius = it.getDimension(R.styleable.EdgeLightingView_edgeHoleRadius, holeRadius)
            holeOffsetX = it.getDimension(R.styleable.EdgeLightingView_edgeHoleOffsetX, holeOffsetX)
            holeOffsetY = it.getDimension(R.styleable.EdgeLightingView_edgeHoleOffsetY, holeOffsetY)
            infinityCurveDepth = it.getDimension(R.styleable.EdgeLightingView_edgeInfinityCurveDepth, infinityCurveDepth)

            val colorsRes = it.getResourceId(R.styleable.EdgeLightingView_edgeColors, 0)
            if (colorsRes != 0) colors = resources.getIntArray(colorsRes)

            val bgColor = it.getColor(R.styleable.EdgeLightingView_edgeBackgroundColor, Color.TRANSPARENT)
            if (bgColor != Color.TRANSPARENT) setBackgroundColorInside(bgColor)

            val bgImageRes = it.getResourceId(R.styleable.EdgeLightingView_edgeBackgroundImage, 0)
            if (bgImageRes != 0) {
                val bmp = BitmapFactory.decodeResource(resources, bgImageRes)
                setBackgroundImage(bmp)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isViewAttached = true
        if (animationEnabled) animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isViewAttached = false
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val path = getCachedPath()
        drawBackground(canvas, path)
        drawEdge(canvas, path)
    }

    private fun getCachedPath(): Path {
        if (cachedPath == null || cachedWidth != width || cachedHeight != height) {
            cachedPath = buildEdgePath()
            cachedWidth = width
            cachedHeight = height
        }
        return cachedPath!!
    }

    private fun drawBackground(canvas: Canvas, path: Path) {
        backgroundInside?.let {
            when (it) {
                is EdgeBackground.Color -> {
                    bgPaint.shader = null
                    bgPaint.color = it.value
                    canvas.drawPath(path, bgPaint)
                }

                is EdgeBackground.Image -> {
                    if (cachedBitmap != it.bitmap) {
                        cachedBitmap = it.bitmap
                        cachedBitmapShader = BitmapShader(it.bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                        updateBgMatrix(width.toFloat(), height.toFloat())
                        cachedBitmapShader?.setLocalMatrix(bgMatrix)
                        bgPaint.shader = cachedBitmapShader
                    } else {
                        bgPaint.shader = cachedBitmapShader
                    }
                    canvas.drawPath(path, bgPaint)
                }
            }
        }
    }

    private fun updateBgMatrix(viewW: Float, viewH: Float) {
        val bmp = cachedBitmap ?: return
        val bmpW = bmp.width.toFloat()
        val bmpH = bmp.height.toFloat()
        bgMatrix.reset()

        when (imageScaleType) {
            EdgeImageScaleType.FIT_XY -> bgMatrix.setScale(viewW / bmpW, viewH / bmpH)
            EdgeImageScaleType.CENTER_CROP -> {
                val scale = maxOf(viewW / bmpW, viewH / bmpH)
                val dx = (viewW - bmpW * scale) / 2f
                val dy = (viewH - bmpH * scale) / 2f
                bgMatrix.setScale(scale, scale)
                bgMatrix.postTranslate(dx, dy)
            }
        }
    }

    private fun drawEdge(canvas: Canvas, path: Path) {
        val shader = getCachedShader()
        edgePaint.shader = shader
        canvas.drawPath(path, edgePaint)
    }

    private fun getCachedShader(): Shader {
        if (cachedShader == null || cachedShaderColors?.contentEquals(colors) != true) {
            cachedShaderColors = colors.copyOf()
            cachedShader = createShader()
        }
        if (animationEnabled) {
            animationMatrix.reset()
            when (direction) {
                EdgeDirection.CLOCKWISE -> animationMatrix.postRotate(progress * 360f, width / 2f, height / 2f)
                EdgeDirection.ANTI_CLOCKWISE -> animationMatrix.postRotate(-progress * 360f, width / 2f, height / 2f)
                EdgeDirection.TOP_TO_BOTTOM -> animationMatrix.postTranslate(0f, height * progress)
                EdgeDirection.BOTTOM_TO_TOP -> animationMatrix.postTranslate(0f, -height * progress)
                EdgeDirection.TOP_RIGHT_TO_BOTTOM_LEFT -> animationMatrix.postTranslate(-width * progress, height * progress)
                EdgeDirection.BOTTOM_LEFT_TO_TOP_RIGHT -> animationMatrix.postTranslate(width * progress, -height * progress)
            }
            cachedShader?.setLocalMatrix(animationMatrix)
        }
        return cachedShader!!
    }

    private fun buildLoopGradient(): Pair<IntArray, FloatArray> {
        val loopColors = IntArray(colors.size + 1)
        for (i in colors.indices) loopColors[i] = colors[i]
        loopColors[colors.size] = colors.first()
        val positions = FloatArray(loopColors.size) { i -> (i + 0.5f) / loopColors.size }
        return loopColors to positions
    }

    private fun createShader(): Shader {
        val w = width.toFloat()
        val h = height.toFloat()
        return when (direction) {
            EdgeDirection.CLOCKWISE, EdgeDirection.ANTI_CLOCKWISE -> {
                val (loopColors, positions) = buildLoopGradient()
                SweepGradient(w / 2f, h / 2f, loopColors, positions)
            }
            EdgeDirection.TOP_RIGHT_TO_BOTTOM_LEFT -> LinearGradient(w, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)
            EdgeDirection.BOTTOM_LEFT_TO_TOP_RIGHT -> LinearGradient(0f, h, w, 0f, colors, null, Shader.TileMode.CLAMP)
            EdgeDirection.TOP_TO_BOTTOM -> LinearGradient(0f, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)
            EdgeDirection.BOTTOM_TO_TOP -> LinearGradient(0f, h, 0f, 0f, colors, null, Shader.TileMode.CLAMP)
        }
    }

    private fun buildEdgePath(): Path {
        val path = Path()
        val pad = strokeWidth / 2f
        val r = width - pad
        val b = height - pad

        when (notchType) {
            NotchType.DEFAULT -> path.addRoundRect(pad, pad, r, b, cornerRadius, cornerRadius, Path.Direction.CW)
            NotchType.DISPLAY_NOTCH -> {
                val notchWidth = width * 0.28f
                val notchHeight = dp(26f)
                val cx = width / 2f
                path.moveTo(pad + cornerRadius, pad)
                path.lineTo(cx - notchWidth / 2, pad)
                path.quadTo(cx, pad + notchHeight, cx + notchWidth / 2, pad)
                path.lineTo(r - cornerRadius, pad)
                path.quadTo(r, pad, r, pad + cornerRadius)
                path.lineTo(r, b - cornerRadius)
                path.quadTo(r, b, r - cornerRadius, b)
                path.lineTo(pad + cornerRadius, b)
                path.quadTo(pad, b, pad, b - cornerRadius)
                path.lineTo(pad, pad + cornerRadius)
                path.quadTo(pad, pad, pad + cornerRadius, pad)
            }
            NotchType.DISPLAY_HOLE -> {
                path.addRoundRect(pad, pad, r, b, cornerRadius, cornerRadius, Path.Direction.CW)
                val cx = width / 2f + holeOffsetX
                val cy = pad + holeOffsetY + holeRadius
                val hole = Path().apply { addCircle(cx, cy, holeRadius, Path.Direction.CW) }
                path.op(hole, Path.Op.DIFFERENCE)
            }
            NotchType.DISPLAY_INFINITY -> {
                val topMid = width / 2f
                path.moveTo(pad + cornerRadius, b)
                path.quadTo(pad, b, pad, b - cornerRadius)
                path.lineTo(pad, pad + cornerRadius)
                path.quadTo(pad, pad, pad + cornerRadius, pad)
                path.lineTo(topMid - cornerRadius * 2, pad)
                path.cubicTo(topMid - cornerRadius, pad, topMid - cornerRadius, pad + infinityCurveDepth, topMid, pad + infinityCurveDepth)
                path.cubicTo(topMid + cornerRadius, pad + infinityCurveDepth, topMid + cornerRadius, pad, topMid + cornerRadius * 2, pad)
                path.lineTo(r - cornerRadius, pad)
                path.quadTo(r, pad, r, pad + cornerRadius)
                path.lineTo(r, b - cornerRadius)
                path.quadTo(r, b, r - cornerRadius, b)
                path.lineTo(pad + cornerRadius, b)
                path.close()
            }
        }
        return path
    }

    fun setBackgroundColorInside(color: Int) {
        backgroundInside = EdgeBackground.Color(color)
        invalidate()
    }

    fun setBackgroundImage(bitmap: Bitmap) {
        backgroundInside = EdgeBackground.Image(bitmap)
        cachedBitmap = null
        invalidate()
    }

    fun setColors(vararg c: Int) {
        colors = c
        cachedShader = null
        invalidate()
    }

    fun setSpeed(ms: Long) {
        duration = ms
        animator.duration = ms
    }

    fun setInfinityCurveDepth(depth: Float) {
        infinityCurveDepth = depth
        cachedPath = null
        invalidate()
    }

    fun setAnimationEnabled(enabled: Boolean) {
        if (animationEnabled == enabled) return
        animationEnabled = enabled
        if (isViewAttached && enabled) {
            progress = 0f
            animator.start()
        } else {
            animator.cancel()
            progress = 0f
            invalidate()
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
