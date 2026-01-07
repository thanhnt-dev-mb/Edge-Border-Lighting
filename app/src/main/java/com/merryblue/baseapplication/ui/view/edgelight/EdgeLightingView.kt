package com.merryblue.baseapplication.ui.view.edgelight

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Xml
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import androidx.core.graphics.PathParser
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.helpers.dpToPx
import org.xmlpull.v1.XmlPullParser
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt

class EdgeLightingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var direction = EdgeDirection.CLOCKWISE
    private var notchType = NotchType.DEFAULT
    private var imageScaleType = EdgeImageScaleType.CENTER_CROP
    private var strokeWidth = 6f.dpToPx
    private var cornerRadius = 24f.dpToPx
    private var duration = 2500L
    private var holeRadius = 14f.dpToPx
    private var holeOffsetX = 0f
    private var holeOffsetY = 10f.dpToPx
    private var infinityCurveDepth = 20f.dpToPx
    private var backgroundInside: EdgeBackground? = null
    private var animationEnabled = true
    private var progress = 0f
    private var patternEnabled = false
    private var patternPath: Path? = null
    private var patternAdvance = 26f.dpToPx
    private var patternSizePx = 14f.dpToPx
    private var patternRotate = true
    private var patternPhase = 0f
    private var patternPhaseMultiplier = 1f
    private var patternVectorResId: Int = 0
    private var patternEvenSpacing = true
    private var lastProgress = 0f
    private var isViewAttached = false
    private var cachedBgPath: Path? = null
    private var cachedOuterEdgePath: Path? = null
    private var cachedPatternEdgePath: Path? = null
    private var cachedWidth = 0
    private var cachedHeight = 0
    private var cachedTotalLength = 0f
    private var contourStarts = FloatArray(0)
    private var contourLens = FloatArray(0)
    private var contourCount = 0
    private val pm = PathMeasure()
    private val pos = FloatArray(2)
    private val tan = FloatArray(2)
    private var cachedShader: Shader? = null
    private var cachedShaderColors: IntArray? = null
    private var cachedShaderW = 0
    private var cachedShaderH = 0
    private val animationMatrix = Matrix()
    private var cachedBitmapShader: BitmapShader? = null
    private var cachedBitmap: Bitmap? = null
    private var cachedBgW = 0
    private var cachedBgH = 0
    private val bgMatrix = Matrix()
    private var colors = intArrayOf(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.CYAN)

    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = this@EdgeLightingView.strokeWidth
        isDither = true
        isFilterBitmap = true
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
        isFilterBitmap = true
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val p = it.animatedValue as Float
            val dp = if (p >= lastProgress) p - lastProgress else (1f - lastProgress) + p
            lastProgress = p

            val len = cachedTotalLength
            if (len > 0f) {
                patternPhase += dp * len * patternPhaseMultiplier
                patternPhase %= len
                if (patternPhase < 0f) patternPhase += len
            } else {
                patternPhase = 0f
            }

            progress = p
            invalidate()
        }
    }

    init {
        attrs?.let { parseAttrs(it) }
        if (patternEnabled && patternVectorResId != 0) applyPatternVector(patternVectorResId, patternSizePx)
        edgePaint.strokeWidth = strokeWidth
        animator.duration = duration
        if (animationEnabled && isViewAttached) animator.start()
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetGeometryCaches()

        cachedShader = null
        cachedShaderW = 0
        cachedShaderH = 0

        if (cachedBitmap != null) {
            cachedBgW = 0
            cachedBgH = 0
            updateBgMatrix(w.toFloat(), h.toFloat())
            cachedBitmapShader?.setLocalMatrix(bgMatrix)
        }
    }

    private fun resetGeometryCaches() {
        cachedBgPath = null
        cachedOuterEdgePath = null
        cachedPatternEdgePath = null

        cachedTotalLength = 0f
        contourCount = 0
        contourStarts = FloatArray(0)
        contourLens = FloatArray(0)

        cachedWidth = 0
        cachedHeight = 0
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        ensurePathsAndGeometry()

        val bgPath = cachedBgPath ?: return
        val edgePath = getEdgePathForDrawing() ?: return

        drawBackground(canvas, bgPath)
        drawEdge(canvas, edgePath)
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

            animationEnabled = it.getBoolean(R.styleable.EdgeLightingView_edgeAnimationEnabled, true)
            strokeWidth = it.getDimension(R.styleable.EdgeLightingView_edgeStrokeWidth, strokeWidth)
            cornerRadius = it.getDimension(R.styleable.EdgeLightingView_edgeCornerRadius, cornerRadius)
            duration = it.getInt(R.styleable.EdgeLightingView_edgeDuration, duration.toInt()).toLong()
            holeRadius = it.getDimension(R.styleable.EdgeLightingView_edgeHoleRadius, holeRadius)
            holeOffsetX = it.getDimension(R.styleable.EdgeLightingView_edgeHoleOffsetX, holeOffsetX)
            holeOffsetY = it.getDimension(R.styleable.EdgeLightingView_edgeHoleOffsetY, holeOffsetY)
            infinityCurveDepth = it.getDimension(R.styleable.EdgeLightingView_edgeInfinityCurveDepth, infinityCurveDepth)

            patternEnabled = it.getBoolean(R.styleable.EdgeLightingView_edgePatternEnabled, false)
            patternRotate = it.getBoolean(R.styleable.EdgeLightingView_edgePatternRotate, true)
            patternAdvance = it.getDimension(R.styleable.EdgeLightingView_edgePatternAdvance, patternAdvance)
            patternSizePx = it.getDimension(R.styleable.EdgeLightingView_edgePatternIconSize, patternSizePx)
            patternPhaseMultiplier = it.getFloat(R.styleable.EdgeLightingView_edgePatternPhaseMultiplier, 1f)
            patternVectorResId = it.getResourceId(R.styleable.EdgeLightingView_edgePatternVector, 0)
            patternEvenSpacing = it.getBoolean(R.styleable.EdgeLightingView_edgePatternEvenSpacing, true)

            val colorsRes = it.getResourceId(R.styleable.EdgeLightingView_edgeColors, 0)
            if (colorsRes != 0) colors = resources.getIntArray(colorsRes)

            val bgColor = it.getColor(R.styleable.EdgeLightingView_edgeBackgroundColor, Color.TRANSPARENT)
            if (bgColor != Color.TRANSPARENT) setBackgroundColorInside(bgColor)

            val bgImageRes = it.getResourceId(R.styleable.EdgeLightingView_edgeBackgroundImage, 0)
            if (bgImageRes != 0) setBackgroundImage(BitmapFactory.decodeResource(resources, bgImageRes))
        }
    }

    private fun ensurePathsAndGeometry() {
        if (cachedWidth == width && cachedHeight == height && cachedBgPath != null) return

        cachedWidth = width
        cachedHeight = height

        cachedBgPath = buildBackgroundPath()
        cachedOuterEdgePath = buildOuterEdgePath()
        cachedPatternEdgePath = buildPatternEdgePath()

        rebuildContourGeometry(getEdgePathForDrawing())
    }

    private fun getEdgePathForDrawing(): Path? {
        return when {
            patternEnabled -> cachedPatternEdgePath
            notchType == NotchType.DISPLAY_HOLE -> cachedOuterEdgePath
            else -> cachedBgPath
        }
    }

    private fun rebuildContourGeometry(edgePath: Path?) {
        if (edgePath == null) {
            cachedTotalLength = 0f
            contourCount = 0
            contourStarts = FloatArray(0)
            contourLens = FloatArray(0)
            return
        }

        pm.setPath(edgePath, false)

        var count = 0
        do {
            if (pm.length > 0.5f) count++
        } while (pm.nextContour())

        contourCount = count
        contourStarts = FloatArray(count)
        contourLens = FloatArray(count)

        pm.setPath(edgePath, false)
        var idx = 0
        var acc = 0f
        do {
            val len = pm.length
            if (len > 0.5f) {
                contourStarts[idx] = acc
                contourLens[idx] = len
                acc += len
                idx++
            }
        } while (pm.nextContour())

        cachedTotalLength = acc
        if (cachedTotalLength < 1f) cachedTotalLength = 0f
    }

    private fun buildBackgroundPath(): Path {
        val path = Path()
        val pad = strokeWidth / 2f
        val r = width - pad
        val b = height - pad

        when (notchType) {
            NotchType.DEFAULT -> {
                path.addRoundRect(pad, pad, r, b, cornerRadius, cornerRadius, Path.Direction.CW)
            }
            NotchType.DISPLAY_NOTCH -> {
                val notchWidth = width * 0.28f
                val notchHeight = 26f.dpToPx
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

    private fun buildOuterEdgePath(): Path {
        val pad = strokeWidth / 2f
        return Path().apply {
            addRoundRect(pad, pad, width - pad, height - pad, cornerRadius, cornerRadius, Path.Direction.CW)
        }
    }

    private fun buildPatternEdgePath(): Path {
        val iconHalf = patternSizePx / 2f
        val inset = (strokeWidth / 2f) + iconHalf
        val rr = (cornerRadius - iconHalf).coerceAtLeast(0f)
        return Path().apply {
            addRoundRect(inset, inset, width - inset, height - inset, rr, rr, Path.Direction.CW)
        }
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
                    val needRecreate = (cachedBitmap != it.bitmap) || (cachedBgW != width) || (cachedBgH != height)
                    if (needRecreate) {
                        cachedBitmap = it.bitmap
                        cachedBgW = width
                        cachedBgH = height
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

    private fun drawEdge(canvas: Canvas, edgePath: Path) {
        edgePaint.shader = getCachedShader()

        val icon = patternPath
        val totalLen = cachedTotalLength
        if (patternEnabled && icon != null && totalLen > 0f && patternAdvance > 0.5f) {
            edgePaint.style = Paint.Style.FILL
            edgePaint.strokeWidth = 0f

            var phase = signedPhase(patternPhase)
            phase %= totalLen
            if (phase < 0f) phase += totalLen

            val desired = patternAdvance
            val count = max(1, (totalLen / desired).roundToInt())
            val effAdv = if (patternEvenSpacing) (totalLen / count.toFloat()) else desired

            for (i in 0 until count) {
                val g = (i * effAdv + phase) % totalLen
                stampAtGlobalDistance(canvas, edgePath, icon, g)
            }
            return
        }

        edgePaint.style = Paint.Style.STROKE
        edgePaint.strokeWidth = strokeWidth
        canvas.drawPath(edgePath, edgePaint)
    }

    private fun stampAtGlobalDistance(canvas: Canvas, edgePath: Path, icon: Path, globalDist: Float) {
        var idx = contourCount - 1
        for (i in 0 until contourCount) {
            val start = contourStarts[i]
            val end = start + contourLens[i]
            if (globalDist in start..<end) {
                idx = i
                break
            }
        }

        val local = globalDist - contourStarts[idx]

        pm.setPath(edgePath, false)
        var c = 0
        while (c < idx && pm.nextContour()) c++

        if (pm.getPosTan(local, pos, tan)) {
            canvas.save()
            canvas.translate(pos[0], pos[1])
            if (patternRotate) {
                val angle = atan2(tan[1], tan[0]) * 180f / Math.PI.toFloat()
                canvas.rotate(angle)
            }
            canvas.drawPath(icon, edgePaint)
            canvas.restore()
        }
    }

    private fun signedPhase(phase: Float): Float {
        return when (direction) {
            EdgeDirection.ANTI_CLOCKWISE,
            EdgeDirection.BOTTOM_TO_TOP,
            EdgeDirection.TOP_RIGHT_TO_BOTTOM_LEFT -> -phase
            else -> phase
        }
    }

    private fun getCachedShader(): Shader {
        val sizeChanged = (cachedShaderW != width || cachedShaderH != height)
        val colorsChanged = (cachedShaderColors?.contentEquals(colors) != true)

        if (cachedShader == null || colorsChanged || sizeChanged) {
            cachedShaderColors = colors.copyOf()
            cachedShader = createShader()
            cachedShaderW = width
            cachedShaderH = height
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
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
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

    private fun applyPatternVector(@DrawableRes resId: Int, iconSizePx: Float) {
        val v = loadVectorAsPath(context, resId)
        patternPath = v.path.centerAndScaleTo(iconSizePx, v.viewportW, v.viewportH)
    }

    private fun loadVectorAsPath(context: Context, @DrawableRes resId: Int): VectorPathResult {
        val parser = context.resources.getXml(resId)
        val attrs = Xml.asAttributeSet(parser)

        var viewportW = 0f
        var viewportH = 0f
        val out = Path()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "vector" -> {
                        for (i in 0 until attrs.attributeCount) {
                            when (attrs.getAttributeName(i)) {
                                "viewportWidth" -> viewportW = attrs.getAttributeValue(i).toFloatOrNull() ?: viewportW
                                "viewportHeight" -> viewportH = attrs.getAttributeValue(i).toFloatOrNull() ?: viewportH
                            }
                        }
                    }
                    "path" -> {
                        var pathData: String? = null
                        for (i in 0 until attrs.attributeCount) {
                            if (attrs.getAttributeName(i) == "pathData") {
                                pathData = attrs.getAttributeValue(i)
                                break
                            }
                        }
                        if (!pathData.isNullOrBlank()) {
                            val p = PathParser.createPathFromPathData(pathData)
                            if (p != null) out.addPath(p)
                        }
                    }
                }
            }
            event = parser.next()
        }

        if (viewportW <= 0f) viewportW = 24f
        if (viewportH <= 0f) viewportH = 24f
        return VectorPathResult(out, viewportW, viewportH)
    }

    private fun Path.centerAndScaleTo(sizePx: Float, viewportW: Float, viewportH: Float): Path {
        val out = Path(this)
        val bounds = out.measureBounds(256)
        out.transform(Matrix().apply { setTranslate(-bounds.centerX(), -bounds.centerY()) })

        val base = maxOf(viewportW, viewportH).coerceAtLeast(1f)
        val s = sizePx / base
        out.transform(Matrix().apply { setScale(s, s) })
        return out
    }

    private fun Path.measureBounds(samples: Int = 256): RectF {
        val pm = PathMeasure(this, false)
        val pos = FloatArray(2)

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        do {
            val len = pm.length
            if (len > 0f) {
                for (i in 0..samples) {
                    val d = len * (i / samples.toFloat())
                    if (pm.getPosTan(d, pos, null)) {
                        val x = pos[0]; val y = pos[1]
                        if (x < minX) minX = x
                        if (y < minY) minY = y
                        if (x > maxX) maxX = x
                        if (y > maxY) maxY = y
                    }
                }
            }
        } while (pm.nextContour())

        if (!minX.isFinite()) return RectF(0f, 0f, 0f, 0f)
        return RectF(minX, minY, maxX, maxY)
    }

    fun setBackgroundColorInside(color: Int) {
        backgroundInside = EdgeBackground.Color(color)
        invalidate()
    }

    fun setBackgroundImage(bitmap: Bitmap) {
        backgroundInside = EdgeBackground.Image(bitmap)
        cachedBitmap = null
        cachedBitmapShader = null
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
        lastProgress = 0f
        patternPhase = 0f
    }

    fun setAnimationEnabled(enabled: Boolean) {
        if (animationEnabled == enabled) return
        animationEnabled = enabled
        if (isViewAttached && enabled) {
            progress = 0f
            lastProgress = 0f
            patternPhase = 0f
            animator.start()
        } else {
            animator.cancel()
            progress = 0f
            lastProgress = 0f
            patternPhase = 0f
            invalidate()
        }
    }

    fun setEdgePatternVector(
        @DrawableRes vectorResId: Int,
        iconSizePx: Float = 14f.dpToPx,
        advancePx: Float = 26f.dpToPx,
        rotate: Boolean = true,
        phaseMultiplier: Float = 1f,
        evenSpacing: Boolean = true
    ) {
        val v = loadVectorAsPath(context, vectorResId)
        val scaled = v.path.centerAndScaleTo(iconSizePx, v.viewportW, v.viewportH)

        patternEnabled = true
        patternVectorResId = vectorResId
        patternSizePx = iconSizePx
        patternPath = scaled
        patternAdvance = advancePx
        patternRotate = rotate
        patternPhaseMultiplier = phaseMultiplier
        patternEvenSpacing = evenSpacing

        resetGeometryCaches()
        invalidate()
    }

    fun setPatternEnabled(enabled: Boolean) {
        patternEnabled = enabled
        resetGeometryCaches()
        invalidate()
    }
}
