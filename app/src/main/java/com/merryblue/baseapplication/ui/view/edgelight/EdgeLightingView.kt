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
import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.coredata.model.edge.EdgePreset
import com.merryblue.baseapplication.coredata.model.edge.EdgeStyle
import com.merryblue.baseapplication.helpers.dpToPx
import org.xmlpull.v1.XmlPullParser
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class EdgeHoleShape { CIRCLE, ROUND }
enum class InfinityShape { U, V }
data class OffsetRange(val min: Float, val max: Float)

class EdgeLightingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var direction = Advanced.DIRECTION_CLOCKWISE
    private var notchType = Advanced.NOTCH_DEFAULT
    private var imageScaleType = EdgeImageScaleType.CENTER_CROP

    private var strokeWidth = 6f.dpToPx
    private var topRadius = 24f.dpToPx
    private var bottomRadius = 24f.dpToPx
    private var duration = 2500L

    private var holeShape: EdgeHoleShape = EdgeHoleShape.CIRCLE

    private var holeRadius = 14f.dpToPx

    private var holeWidthPx = 64f.dpToPx
    private var holeHeightPx = 28f.dpToPx
    private var holeCornerRadiusPx = 14f.dpToPx

    private var holeOffsetX = 0f
    private var holeOffsetY = 40f.dpToPx

    private var infinityShape: InfinityShape = InfinityShape.U
    private var infinityWidthPx: Float = 0f
    private var infinityHeightPx: Float = 20f.dpToPx
    private var infinityRadiusTopPx: Float = 0f
    private var infinityRadiusBottomPx: Float = 0f

    private var notchBottomCapsuleBias: Float = 0f
    private var notchWidthPx: Float = 0f
    private var notchWidthFraction: Float = 0.35677505f
    private var notchHeightPx: Float = 73.51973f.dpToPx
    private var notchTopRadiusPx: Float = 39.304764f.dpToPx
    private var notchBottomRadiusPx: Float = 29.377974f.dpToPx
    private var notchBottomFullness: Float = 0f

    private var backgroundInside: EdgeBackground? = null

    private var animationEnabled = true
    private var progress = 0f

    private var patternEnabled = false
    private var patternPath: Path? = null
    private var patternVectorResId: Int = 0
    private var patternRotate = true
    private var patternPhase = 0f
    private var patternPhaseMultiplier = 1f
    private var patternEvenSpacing = true
    private var patternSizePx = 14f.dpToPx
    private var patternAdvance = 26f.dpToPx
    private var patternGapPx = (patternAdvance - patternSizePx).coerceAtLeast(0f)
    private var patternInsetExtraPx = 0f

    private var lastProgress = 0f
    private var isViewAttached = false

    private var cachedBgPath: Path? = null
    private var cachedOuterEdgePath: Path? = null
    private var cachedPatternEdgePath: Path? = null
    private var cachedHoleEdgePath: Path? = null
    private var cachedPatternHoleEdgePath: Path? = null

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

        patternGapPx = (patternAdvance - patternSizePx).coerceAtLeast(0f)
        patternAdvance = patternSizePx + patternGapPx

        if (patternEnabled && patternVectorResId != 0) {
            applyPatternVector(patternVectorResId, patternSizePx)
        }

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
        cachedHoleEdgePath = null
        cachedPatternHoleEdgePath = null

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
                1 -> Advanced.DIRECTION_ANTI_CLOCKWISE
                2 -> Advanced.DIRECTION_TOP_RIGHT_BOTTOM_LEFT
                3 -> Advanced.DIRECTION_BOTTOM_LEFT_TOP_RIGHT
                4 -> Advanced.DIRECTION_DOWN
                5 -> Advanced.DIRECTION_UP
                else -> Advanced.DIRECTION_CLOCKWISE
            }

            notchType = when (it.getInt(R.styleable.EdgeLightingView_edgeNotchType, 0)) {
                1 -> Advanced.NOTCH_DISPLAY_NOTCH
                2 -> Advanced.NOTCH_DISPLAY_HOLE
                3 -> Advanced.NOTCH_DISPLAY_INFINITY
                else -> Advanced.NOTCH_DEFAULT
            }

            imageScaleType = when (it.getInt(R.styleable.EdgeLightingView_edgeImageScaleType, 1)) {
                0 -> EdgeImageScaleType.FIT_XY
                else -> EdgeImageScaleType.CENTER_CROP
            }

            animationEnabled = it.getBoolean(R.styleable.EdgeLightingView_edgeAnimationEnabled, true)

            strokeWidth = it.getDimension(R.styleable.EdgeLightingView_edgeStrokeWidth, strokeWidth)
            topRadius = it.getDimension(R.styleable.EdgeLightingView_edgeTopCornerRadius, topRadius)
            bottomRadius = it.getDimension(R.styleable.EdgeLightingView_edgeBottomCornerRadius, bottomRadius)

            duration = it.getInt(R.styleable.EdgeLightingView_edgeDuration, duration.toInt()).toLong()

            holeRadius = it.getDimension(R.styleable.EdgeLightingView_edgeHoleRadius, holeRadius)
            holeOffsetX = it.getDimension(R.styleable.EdgeLightingView_edgeHoleOffsetX, holeOffsetX)
            holeOffsetY = it.getDimension(R.styleable.EdgeLightingView_edgeHoleOffsetY, holeOffsetY)

            patternEnabled = it.getBoolean(R.styleable.EdgeLightingView_edgePatternEnabled, false)
            patternRotate = it.getBoolean(R.styleable.EdgeLightingView_edgePatternRotate, true)
            patternAdvance = it.getDimension(R.styleable.EdgeLightingView_edgePatternAdvance, patternAdvance)
            patternSizePx = it.getDimension(R.styleable.EdgeLightingView_edgePatternIconSize, patternSizePx)
            patternPhaseMultiplier = it.getFloat(R.styleable.EdgeLightingView_edgePatternPhaseMultiplier, 1f)
            patternVectorResId = it.getResourceId(R.styleable.EdgeLightingView_edgePatternVector, 0)
            patternEvenSpacing = it.getBoolean(R.styleable.EdgeLightingView_edgePatternEvenSpacing, true)

            notchWidthPx = it.getDimension(R.styleable.EdgeLightingView_edgeNotchWidthPx, notchWidthPx)
            notchWidthFraction = it.getFloat(R.styleable.EdgeLightingView_edgeNotchWidthFraction, notchWidthFraction)
            notchHeightPx = it.getDimension(R.styleable.EdgeLightingView_edgeNotchHeight, notchHeightPx)
            notchTopRadiusPx = it.getDimension(R.styleable.EdgeLightingView_edgeNotchTopRadius, notchTopRadiusPx)
            notchBottomRadiusPx = it.getDimension(R.styleable.EdgeLightingView_edgeNotchBottomRadius, notchBottomRadiusPx)
            notchBottomFullness = it.getFloat(
                R.styleable.EdgeLightingView_edgeNotchBottomFullness,
                notchBottomFullness
            ).coerceIn(0f, 1f)

            val colorsRes = it.getResourceId(R.styleable.EdgeLightingView_edgeColors, 0)
            if (colorsRes != 0) colors = resources.getIntArray(colorsRes)

            val bgColor = it.getColor(R.styleable.EdgeLightingView_edgeBackgroundColor, Color.TRANSPARENT)
            if (bgColor != Color.TRANSPARENT) setBackgroundColorInside(bgColor)

            val bgImageRes = it.getResourceId(R.styleable.EdgeLightingView_edgeBackgroundImage, 0)
            if (bgImageRes != 0) setBackgroundImage(bgImageRes)
        }

        notchWidthFraction = notchWidthFraction.coerceIn(0.05f, 0.95f)
        patternGapPx = (patternAdvance - patternSizePx).coerceAtLeast(0f)
        patternAdvance = patternSizePx + patternGapPx
    }

    private fun ensurePathsAndGeometry() {
        if (cachedWidth == width && cachedHeight == height && cachedBgPath != null) return

        cachedWidth = width
        cachedHeight = height

        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) {
            clampHoleOffsetsForCurrentConfig()
        }

        cachedBgPath = buildBackgroundPath()
        cachedOuterEdgePath = buildOuterEdgePath()
        cachedPatternEdgePath = buildPatternEdgePath()

        cachedHoleEdgePath = if (notchType == Advanced.NOTCH_DISPLAY_HOLE) buildHoleEdgePath() else null
        cachedPatternHoleEdgePath = if (notchType == Advanced.NOTCH_DISPLAY_HOLE) buildPatternHoleEdgePath() else null

        rebuildContourGeometry(getEdgePathForDrawing())
    }

    private fun getEdgePathForDrawing(): Path? {
        return when {
            patternEnabled && notchType == Advanced.NOTCH_DISPLAY_HOLE -> cachedPatternHoleEdgePath
            patternEnabled -> cachedPatternEdgePath
            notchType == Advanced.NOTCH_DISPLAY_HOLE -> cachedHoleEdgePath
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

        val tr = topRadius.coerceAtLeast(0f)
        val br = bottomRadius.coerceAtLeast(0f)

        fun addRoundRectTopBottom(left: Float, top: Float, right: Float, bottom: Float, topR: Float, bottomR: Float) {
            val radii = floatArrayOf(
                topR, topR,       // TL
                topR, topR,       // TR
                bottomR, bottomR, // BR
                bottomR, bottomR  // BL
            )
            path.addRoundRect(RectF(left, top, right, bottom), radii, Path.Direction.CW)
        }

        when (notchType) {
            Advanced.NOTCH_DEFAULT -> {
                addRoundRectTopBottom(pad, pad, r, b, tr, br)
            }

            Advanced.NOTCH_DISPLAY_HOLE -> {
                addRoundRectTopBottom(pad, pad, r, b, tr, br)
                val hole = buildHolePathForBackground(inset = pad)
                path.op(hole, Path.Op.DIFFERENCE)
            }

            Advanced.NOTCH_DISPLAY_NOTCH -> {
                val topCornerR = tr.coerceAtMost((r - pad) / 2f)
                val bottomCornerR = br.coerceAtMost((b - pad) / 2f)

                buildNotchConcavePath(
                    out = path,
                    left = pad,
                    top = pad,
                    right = r,
                    bottom = b,
                    topCornerR = topCornerR,
                    bottomCornerR = bottomCornerR,
                    notchWidthPxOr0 = notchWidthPx,
                    notchWidthFraction = notchWidthFraction,
                    notchHeight = notchHeightPx,
                    notchTopRadius = notchTopRadiusPx,
                    notchBottomRadius = notchBottomRadiusPx,
                    notchBottomFullness = notchBottomFullness
                )
            }

            Advanced.NOTCH_DISPLAY_INFINITY -> {
                val left = pad
                val top = pad
                val right = r
                val bottom = b

                val topMid = width / 2f
                val topR = tr.coerceAtMost((right - left) / 2f)
                val botR = br.coerceAtMost((bottom - top) / 2f)

                path.reset()

                path.moveTo(left + botR, bottom)
                path.quadTo(left, bottom, left, bottom - botR)
                path.lineTo(left, top + topR)
                path.quadTo(left, top, left + topR, top)

                val (infW, infH, rTop) = resolveInfinityParams(
                    inset = pad,
                    outerTopRadius = topR,
                    outerBottomRadius = botR
                )

                val halfW = infW / 2f
                val startX = (topMid - halfW).coerceAtLeast(left + topR)
                val endX = (topMid + halfW).coerceAtMost(right - topR)

                path.lineTo(startX, top)

                when (infinityShape) {
                    InfinityShape.U -> buildInfinityUPath(
                        out = path,
                        startX = startX,
                        endX = endX,
                        topY = top,
                        bottomY = top + infH,
                        radiusTop = rTop
                    )

                    InfinityShape.V -> buildInfinityVPath(
                        out = path,
                        startX = startX,
                        endX = endX,
                        topY = top,
                        bottomY = top + infH,
                        radiusTop = rTop,
                    )
                }

                path.lineTo(right - topR, top)
                path.quadTo(right, top, right, top + topR)

                path.lineTo(right, bottom - botR)
                path.quadTo(right, bottom, right - botR, bottom)

                path.lineTo(left + botR, bottom)
                path.close()
            }

            else -> Unit
        }

        return path
    }

    private fun buildOuterEdgePath(): Path {
        val pad = strokeWidth / 2f
        val tr = topRadius.coerceAtLeast(0f)
        val br = bottomRadius.coerceAtLeast(0f)

        val radii = floatArrayOf(tr, tr, tr, tr, br, br, br, br)

        return Path().apply {
            addRoundRect(RectF(pad, pad, width - pad, height - pad), radii, Path.Direction.CW)
        }
    }

    private fun buildHoleEdgePath(): Path {
        val pad = strokeWidth / 2f
        val tr = topRadius.coerceAtLeast(0f)
        val br = bottomRadius.coerceAtLeast(0f)
        val radii = floatArrayOf(tr, tr, tr, tr, br, br, br, br)

        return Path().apply {
            addRoundRect(RectF(pad, pad, width - pad, height - pad), radii, Path.Direction.CW)
            addPath(buildHolePathForStroke(inset = pad))
        }
    }

    private fun buildPatternEdgePath(): Path {
        return when (notchType) {
            Advanced.NOTCH_DEFAULT -> buildPatternDefaultPath()
            Advanced.NOTCH_DISPLAY_NOTCH -> buildPatternNotchPath()
            Advanced.NOTCH_DISPLAY_INFINITY -> buildPatternInfinityPath()
            Advanced.NOTCH_DISPLAY_HOLE -> buildPatternDefaultPath()
            else -> buildPatternDefaultPath()
        }
    }

    private fun buildPatternDefaultPath(): Path {
        val iconHalf = patternSizePx / 2f
        val inset = iconHalf + patternInsetExtraPx

        val tr = (topRadius - iconHalf).coerceAtLeast(0f)
        val br = (bottomRadius - iconHalf).coerceAtLeast(0f)

        val radii = floatArrayOf(tr, tr, tr, tr, br, br, br, br)

        return Path().apply {
            addRoundRect(RectF(inset, inset, width - inset, height - inset), radii, Path.Direction.CW)
        }
    }

    private fun buildPatternNotchPath(): Path {
        val out = Path()

        val iconHalf = patternSizePx / 2f
        val inset = iconHalf + patternInsetExtraPx

        val right = width - inset
        val bottom = height - inset

        val usableW = (right - inset).coerceAtLeast(1f)
        val topCornerR = (topRadius - iconHalf).coerceAtLeast(0f).coerceAtMost(usableW / 2f)
        val bottomCornerR = (bottomRadius - iconHalf).coerceAtLeast(0f).coerceAtMost((bottom - inset) / 2f)

        val notchTopR = (notchTopRadiusPx - iconHalf * 0.4f).coerceAtLeast(0f)
        val notchBotR = (notchBottomRadiusPx - iconHalf * 0.4f).coerceAtLeast(0f)
        val notchH = (notchHeightPx - iconHalf).coerceAtLeast(0f)

        buildNotchConcavePath(
            out = out,
            left = inset,
            top = inset,
            right = right,
            bottom = bottom,
            topCornerR = topCornerR,
            bottomCornerR = bottomCornerR,
            notchWidthPxOr0 = if (notchWidthPx > 0f) (notchWidthPx - iconHalf * 2f).coerceAtLeast(0f) else 0f,
            notchWidthFraction = notchWidthFraction,
            notchHeight = notchH,
            notchTopRadius = notchTopR,
            notchBottomRadius = notchBotR,
            notchBottomFullness = notchBottomFullness
        )

        return out
    }

    private fun buildPatternInfinityPath(): Path {
        val path = Path()

        val iconHalf = patternSizePx / 2f
        val inset = iconHalf + patternInsetExtraPx

        val left = inset
        val top = inset
        val right = width - inset
        val bottom = height - inset

        val topMid = width / 2f

        val usableW = (right - left).coerceAtLeast(1f)
        val topR = (topRadius - iconHalf).coerceAtLeast(0f).coerceAtMost(usableW / 2f)
        val botR = (bottomRadius - iconHalf).coerceAtLeast(0f).coerceAtMost((bottom - top) / 2f)

        val (infW, infH, rTop, rBot) = resolveInfinityParams(
            inset = inset,
            outerTopRadius = topR,
            outerBottomRadius = botR
        )

        val halfW = infW / 2f
        val startX = (topMid - halfW).coerceAtLeast(left + topR)
        val endX = (topMid + halfW).coerceAtMost(right - topR)

        path.moveTo(left + botR, bottom)
        path.quadTo(left, bottom, left, bottom - botR)
        path.lineTo(left, top + topR)
        path.quadTo(left, top, left + topR, top)

        path.lineTo(startX, top)

        when (infinityShape) {
            InfinityShape.U -> buildInfinityUPath(path, startX, endX, top, top + infH, rTop)
            InfinityShape.V -> buildInfinityVPath(path, startX, endX, top, top + infH, rTop)
        }

        path.lineTo(right - topR, top)
        path.quadTo(right, top, right, top + topR)
        path.lineTo(right, bottom - botR)
        path.quadTo(right, bottom, right - botR, bottom)
        path.lineTo(left + botR, bottom)
        path.close()

        return path
    }

    private fun buildPatternHoleEdgePath(): Path {
        val iconHalf = patternSizePx / 2f
        val inset = iconHalf + patternInsetExtraPx

        val tr = (topRadius - iconHalf).coerceAtLeast(0f)
        val br = (bottomRadius - iconHalf).coerceAtLeast(0f)

        val radii = floatArrayOf(tr, tr, tr, tr, br, br, br, br)

        return Path().apply {
            addRoundRect(RectF(inset, inset, width - inset, height - inset), radii, Path.Direction.CW)
            addPath(buildHolePathForPattern(inset = inset, iconHalf = iconHalf))
        }
    }

    private fun buildNotchConcavePath(
        out: Path,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        topCornerR: Float,
        bottomCornerR: Float,
        notchWidthPxOr0: Float,
        notchWidthFraction: Float,
        notchHeight: Float,
        notchTopRadius: Float,
        notchBottomRadius: Float,
        notchBottomFullness: Float
    ) {
        val usableW = (right - left).coerceAtLeast(1f)
        val cx = (left + right) / 2f

        val notchW = (if (notchWidthPxOr0 > 0f) notchWidthPxOr0 else usableW * notchWidthFraction)
            .coerceIn(0f, usableW)

        val notchL0 = cx - notchW / 2f
        val notchR0 = cx + notchW / 2f

        val nBot = (top + notchHeight).coerceAtMost(bottom - bottomCornerR)

        val maxRX = (notchR0 - notchL0) / 2f
        val maxRY = (nBot - top) / 2f

        val rTop = notchTopRadius.coerceAtLeast(0f).coerceAtMost(minOf(maxRX, maxRY))
        val rBotRaw = notchBottomRadius.coerceAtLeast(0f).coerceAtMost(minOf(maxRX, maxRY))

        val notchL = notchL0.coerceIn(left + topCornerR + rTop, right - topCornerR - rTop)
        val notchR = notchR0.coerceIn(left + topCornerR + rTop, right - topCornerR - rTop)

        val halfW = (notchR - notchL) / 2f
        val rBot = rBotRaw.coerceAtMost((halfW - 0.01f).coerceAtLeast(0f))

        val f = notchBottomFullness.coerceIn(0f, 1f)

        val dipMax = (notchHeight * 0.35f).coerceAtLeast(0f)
        val spanForFactor = ((notchR - rBot) - (notchL + rBot)).coerceAtLeast(1f)
        val radiusFactor = (if (spanForFactor > 0f) (rBot / (spanForFactor / 2f)) else 0f).coerceIn(0f, 1f)

        val dip = (dipMax * f * (1f - radiusFactor * 0.85f)).coerceAtLeast(0f)

        val maxDipBySpace = ((bottom - bottomCornerR) - nBot - 1f).coerceAtLeast(0f)
        val dipClamped = dip.coerceAtMost(maxDipBySpace)

        out.reset()

        out.moveTo(left + topCornerR, top)
        out.lineTo(notchL - rTop, top)
        out.quadTo(notchL, top, notchL, top + rTop)
        out.lineTo(notchL, nBot - rBot)

        val kappa = 0.5522848f
        val p0y = nBot - rBot
        val p3y = nBot - rBot

        val x1 = notchL + rBot
        val x2 = notchR - rBot
        val midX = (x1 + x2) / 2f

        if (rBot > 0.001f) {
            out.cubicTo(
                notchL, p0y + kappa * rBot,
                x1 - kappa * rBot, nBot,
                x1, nBot
            )
        } else {
            out.lineTo(x1, nBot)
        }

        val span = (x2 - x1).coerceAtLeast(1f)
        val t = span * 0.25f

        val bias = notchBottomCapsuleBias.coerceIn(0f, 1f)
        val midFactor = (1f - 0.85f * bias).coerceIn(0f, 1f)
        val yMid = nBot + dipClamped * midFactor
        val cMidDown = nBot + (yMid - nBot) + 0.90f

        out.cubicTo(
            x1 + t, nBot,
            midX - t, cMidDown,
            midX, yMid
        )

        out.cubicTo(
            midX + t, cMidDown,
            x2 - t, nBot,
            x2, nBot
        )

        if (rBot > 0.001f) {
            out.cubicTo(
                x2 + kappa * rBot, nBot,
                notchR, p3y + kappa * rBot,
                notchR, p3y
            )
        } else {
            out.lineTo(notchR, p3y)
        }

        out.lineTo(notchR, top + rTop)
        out.quadTo(notchR, top, notchR + rTop, top)

        out.lineTo(right - topCornerR, top)
        out.quadTo(right, top, right, top + topCornerR)

        out.lineTo(right, bottom - bottomCornerR)
        out.quadTo(right, bottom, right - bottomCornerR, bottom)

        out.lineTo(left + bottomCornerR, bottom)
        out.quadTo(left, bottom, left, bottom - bottomCornerR)

        out.lineTo(left, top + topCornerR)
        out.quadTo(left, top, left + topCornerR, top)

        out.close()
    }

    private fun drawBackground(canvas: Canvas, path: Path) {
        val bg = backgroundInside ?: return
        when (bg) {
            is EdgeBackground.Color -> {
                bgPaint.shader = null
                bgPaint.color = bg.value
                canvas.drawPath(path, bgPaint)
            }
            is EdgeBackground.Image -> {
                val needRecreate = (cachedBitmap != bg.bitmap) || (cachedBgW != width) || (cachedBgH != height)
                if (needRecreate) {
                    cachedBitmap = bg.bitmap
                    cachedBgW = width
                    cachedBgH = height
                    cachedBitmapShader = BitmapShader(bg.bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
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
        if (contourCount <= 0) return

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
            Advanced.DIRECTION_ANTI_CLOCKWISE,
            Advanced.DIRECTION_UP,
            Advanced.DIRECTION_TOP_RIGHT_BOTTOM_LEFT -> -phase
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
                Advanced.DIRECTION_CLOCKWISE -> animationMatrix.postRotate(progress * 360f, width / 2f, height / 2f)
                Advanced.DIRECTION_ANTI_CLOCKWISE -> animationMatrix.postRotate(-progress * 360f, width / 2f, height / 2f)
                Advanced.DIRECTION_DOWN -> animationMatrix.postTranslate(0f, height * progress)
                Advanced.DIRECTION_UP -> animationMatrix.postTranslate(0f, -height * progress)
                Advanced.DIRECTION_TOP_RIGHT_BOTTOM_LEFT -> animationMatrix.postTranslate(-width * progress, height * progress)
                Advanced.DIRECTION_BOTTOM_LEFT_TOP_RIGHT -> animationMatrix.postTranslate(width * progress, -height * progress)
                else -> {}
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
            Advanced.DIRECTION_CLOCKWISE, Advanced.DIRECTION_ANTI_CLOCKWISE -> {
                val (loopColors, positions) = buildLoopGradient()
                SweepGradient(w / 2f, h / 2f, loopColors, positions)
            }
            Advanced.DIRECTION_TOP_RIGHT_BOTTOM_LEFT -> LinearGradient(w, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)
            Advanced.DIRECTION_BOTTOM_LEFT_TOP_RIGHT -> LinearGradient(0f, h, w, 0f, colors, null, Shader.TileMode.CLAMP)
            Advanced.DIRECTION_DOWN -> LinearGradient(0f, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)
            Advanced.DIRECTION_UP -> LinearGradient(0f, h, 0f, 0f, colors, null, Shader.TileMode.CLAMP)
            else -> {
                val (loopColors, positions) = buildLoopGradient()
                SweepGradient(w / 2f, h / 2f, loopColors, positions)
            }
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
                            out.addPath(p)
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

    private fun buildHolePathForBackground(inset: Float): Path {
        val cx = width / 2f + holeOffsetX
        val cy = inset + holeOffsetY

        return when (holeShape) {
            EdgeHoleShape.CIRCLE -> {
                Path().apply { addCircle(cx, cy, holeRadius.coerceAtLeast(0f), Path.Direction.CW) }
            }
            EdgeHoleShape.ROUND -> {
                val w = holeWidthPx.coerceAtLeast(0f)
                val h = holeHeightPx.coerceAtLeast(0f)
                val left = cx - w / 2f
                val top = cy - h / 2f
                val rect = RectF(left, top, left + w, top + h)
                val cr = holeCornerRadiusPx.coerceAtLeast(0f).coerceAtMost(minOf(w, h) / 2f)
                Path().apply { addRoundRect(rect, cr, cr, Path.Direction.CW) }
            }
        }
    }

    private fun buildHolePathForStroke(inset: Float): Path = buildHolePathForBackground(inset)

    private fun buildHolePathForPattern(inset: Float, iconHalf: Float): Path {
        val cx = width / 2f + holeOffsetX
        val cy = inset + holeOffsetY

        return when (holeShape) {
            EdgeHoleShape.CIRCLE -> {
                val r = (holeRadius - iconHalf).coerceAtLeast(0f)
                Path().apply { if (r > 0.5f) addCircle(cx, cy, r, Path.Direction.CW) }
            }
            EdgeHoleShape.ROUND -> {
                val w = (holeWidthPx - iconHalf * 2f).coerceAtLeast(0f)
                val h = (holeHeightPx - iconHalf * 2f).coerceAtLeast(0f)
                val left = cx - w / 2f
                val top = cy - h / 2f
                val rect = RectF(left, top, left + w, top + h)

                val cr = (holeCornerRadiusPx - iconHalf * 0.8f)
                    .coerceAtLeast(0f)
                    .coerceAtMost(minOf(w, h) / 2f)

                Path().apply { if (w > 0.5f && h > 0.5f) addRoundRect(rect, cr, cr, Path.Direction.CW) }
            }
        }
    }

    private data class HoleCenterBounds(
        val inset: Float,
        val minCx: Float,
        val maxCx: Float,
        val minCy: Float,
        val maxCy: Float
    )

    private fun computeHoleCenterBounds(): HoleCenterBounds? {
        if (width <= 0 || height <= 0) return null
        if (notchType != Advanced.NOTCH_DISPLAY_HOLE) return null

        val pad = strokeWidth / 2f
        val patternInset = if (patternEnabled) (patternSizePx / 2f + patternInsetExtraPx) else pad
        val inset = maxOf(pad, patternInset)

        val tr = topRadius.coerceAtLeast(0f)
        val br = bottomRadius.coerceAtLeast(0f)

        val cornerClearX = maxOf(tr, br)

        val halfW: Float
        val halfH: Float
        when (holeShape) {
            EdgeHoleShape.CIRCLE -> {
                halfW = holeRadius.coerceAtLeast(0f)
                halfH = holeRadius.coerceAtLeast(0f)
            }
            EdgeHoleShape.ROUND -> {
                halfW = (holeWidthPx / 2f).coerceAtLeast(0f)
                halfH = (holeHeightPx / 2f).coerceAtLeast(0f)
            }
        }

        val minCx = inset + cornerClearX + halfW
        val maxCx = (width - inset - cornerClearX - halfW).coerceAtLeast(minCx)

        val minCy = inset + tr + halfH
        val maxCy = (height - inset - br - halfH).coerceAtLeast(minCy)

        return HoleCenterBounds(inset, minCx, maxCx, minCy, maxCy)
    }

    private fun clampHoleOffsetsForCurrentConfig() {
        val b = computeHoleCenterBounds() ?: return

        val curCx = width / 2f + holeOffsetX
        val curCy = b.inset + holeOffsetY

        val clampedCx = curCx.coerceIn(b.minCx, b.maxCx)
        val clampedCy = curCy.coerceIn(b.minCy, b.maxCy)

        holeOffsetX = clampedCx - width / 2f
        holeOffsetY = clampedCy - b.inset
    }

    fun getHoleOffsetXRangePx(): OffsetRange {
        val b = computeHoleCenterBounds() ?: return OffsetRange(0f, 0f)
        return OffsetRange(b.minCx - width / 2f, b.maxCx - width / 2f)
    }

    fun getHoleOffsetYRangePx(): OffsetRange {
        val b = computeHoleCenterBounds() ?: return OffsetRange(0f, 0f)
        return OffsetRange(b.minCy - b.inset, b.maxCy - b.inset)
    }

    fun setHoleOffsetXProgress(p: Float) {
        val r = getHoleOffsetXRangePx()
        setHoleOffsetX(r.min + (r.max - r.min) * p.coerceIn(0f, 1f))
    }

    fun setHoleOffsetYProgress(p: Float) {
        val r = getHoleOffsetYRangePx()
        setHoleOffsetY(r.min + (r.max - r.min) * p.coerceIn(0f, 1f))
    }

    private fun resolveInfinityParams(
        inset: Float,
        outerTopRadius: Float,
        outerBottomRadius: Float
    ): FloatArray {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)

        val maxAvailableW = (w - 2f * inset - 2f * outerTopRadius).coerceAtLeast(10f)
        val autoW = (w * 0.42f).coerceIn(60f.dpToPx, maxAvailableW)
        val infW = (if (infinityWidthPx > 0f) infinityWidthPx else autoW).coerceIn(10f, maxAvailableW)

        val maxH = (h - inset - outerBottomRadius - inset - 1f).coerceAtLeast(0f)
        val infH = infinityHeightPx.coerceAtLeast(0f).coerceAtMost(maxH)

        val halfW = infW / 2f

        val autoTopR = outerTopRadius.coerceAtLeast(0f)
        val rTop = (if (infinityRadiusTopPx > 0f) infinityRadiusTopPx else autoTopR)
            .coerceIn(0f, min(halfW, infH))

        val rBot = 0f

        return floatArrayOf(infW, infH, rTop, rBot)
    }

    private fun buildInfinityUPath(
        out: Path,
        startX: Float,
        endX: Float,
        topY: Float,
        bottomY: Float,
        radiusTop: Float
    ) {
        val w = (endX - startX).coerceAtLeast(0f)
        val h = (bottomY - topY).coerceAtLeast(0f)
        if (w < 1f || h < 1f) {
            out.lineTo(endX, topY)
            return
        }

        val cx = (startX + endX) / 2f
        val rTop = radiusTop.coerceIn(0f, minOf(w / 2f, h))
        val k = 0.5522848f

        if (rTop > 0.5f) {
            out.cubicTo(
                startX + k * rTop, topY,
                startX + rTop, topY + (1f - k) * rTop,
                startX + rTop, topY + rTop
            )
        } else {
            out.lineTo(startX, topY + 0.25f * h)
        }

        val leftJoinX = startX + rTop
        val rightJoinX = endX - rTop

        val c1x = leftJoinX
        val c1y = topY + 0.85f * h
        val c2x = cx - 0.25f * w
        val c2y = bottomY

        out.cubicTo(
            c1x, c1y,
            c2x, c2y,
            cx, bottomY
        )

        val c3x = cx + 0.25f * w
        val c3y = bottomY
        val c4x = rightJoinX
        val c4y = topY + 0.85f * h

        out.cubicTo(
            c3x, c3y,
            c4x, c4y,
            rightJoinX, topY + rTop
        )

        if (rTop > 0.5f) {
            out.cubicTo(
                endX - rTop, topY + k * rTop,
                endX - k * rTop, topY,
                endX, topY
            )
        } else {
            out.lineTo(endX, topY)
        }
    }


    private fun buildInfinityVPath(
        out: Path,
        startX: Float,
        endX: Float,
        topY: Float,
        bottomY: Float,
        radiusTop: Float,
    ) {
        val w = (endX - startX).coerceAtLeast(0f)
        val h = (bottomY - topY).coerceAtLeast(0f)
        if (w < 1f || h < 1f) {
            out.lineTo(endX, topY)
            return
        }

        val cx = (startX + endX) / 2f
        val k = 0.5522848f

        val rTop = radiusTop.coerceIn(0f, minOf(w / 2f, h))

        val mouthLx = startX + rTop
        val mouthRx = endX - rTop
        val mouthY = topY + rTop

        val tipX = cx
        val tipY = bottomY

        if (rTop > 0.5f) {
            out.cubicTo(
                startX + k * rTop, topY,
                startX + rTop, topY + (1f - k) * rTop,
                mouthLx, mouthY
            )
        } else {
            out.lineTo(startX, topY + 0.25f * h)
            out.lineTo(mouthLx, mouthY)
        }

        out.lineTo(tipX, tipY)
        out.lineTo(mouthRx, mouthY)

        if (rTop > 0.5f) {
            out.cubicTo(
                endX - rTop, topY + k * rTop,
                endX - k * rTop, topY,
                endX, topY
            )
        } else {
            out.lineTo(endX, topY)
        }
    }

    private fun setBackgroundColorInsideInternal(color: Int) {
        backgroundInside = EdgeBackground.Color(color)
    }

    private fun setBackgroundImageInternal(@DrawableRes resId: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        backgroundInside = EdgeBackground.Image(bitmap)
        cachedBitmap = null
        cachedBitmapShader = null
    }

    private fun setPatternEnabledInternal(enabled: Boolean) {
        patternEnabled = enabled
        resetGeometryCaches()
    }

    private fun setColorsInternal(c: IntArray) {
        colors = c
        cachedShader = null
    }

    private fun applyEdgeStyleInternal(edge: EdgeStyle) {
        when (edge) {
            is EdgeStyle.LinearColor -> {
                if (patternEnabled) setPatternEnabledInternal(false)
                setColorsInternal(edge.colors)
                resetGeometryCaches()
            }
            is EdgeStyle.Pattern -> {
                setEdgePatternVectorInternal(
                    vectorResId = edge.vectorResId,
                    iconSizePx = edge.iconSizePx,
                    rotate = edge.rotate,
                    phaseMultiplier = edge.phaseMultiplier,
                    evenSpacing = edge.evenSpacing,
                    gapPx = patternGapPx
                )
            }
            EdgeStyle.None -> {
                if (patternEnabled) setPatternEnabledInternal(false)
                resetGeometryCaches()
            }
        }
    }

    private fun setEdgePatternVectorInternal(
        @DrawableRes vectorResId: Int,
        iconSizePx: Float,
        rotate: Boolean,
        phaseMultiplier: Float,
        evenSpacing: Boolean,
        gapPx: Float
    ) {
        val v = loadVectorAsPath(context, vectorResId)
        val scaled = v.path.centerAndScaleTo(iconSizePx, v.viewportW, v.viewportH)

        patternEnabled = true
        patternVectorResId = vectorResId
        patternSizePx = iconSizePx.coerceAtLeast(0.5f)
        patternPath = scaled
        patternRotate = rotate
        patternPhaseMultiplier = phaseMultiplier
        patternEvenSpacing = evenSpacing

        patternGapPx = gapPx.coerceAtLeast(0f)
        patternAdvance = patternSizePx + patternGapPx

        resetGeometryCaches()
    }

    private fun resetAnimationState() {
        progress = 0f
        lastProgress = 0f
        patternPhase = 0f
    }

    private fun setLinearStrokeWidthOnlyPx(px: Float) {
        val v = px.coerceAtLeast(0.5f)
        if (strokeWidth == v) return
        strokeWidth = v
        edgePaint.strokeWidth = v
        resetGeometryCaches()
        invalidate()
    }

    private fun setPatternIconSizeOnlyPx(iconSizePx: Float) {
        val v = iconSizePx.coerceAtLeast(0.5f)
        if (patternSizePx == v) return

        patternSizePx = v
        patternAdvance = patternSizePx + patternGapPx

        if (patternVectorResId != 0) {
            val vp = loadVectorAsPath(context, patternVectorResId)
            patternPath = vp.path.centerAndScaleTo(patternSizePx, vp.viewportW, vp.viewportH)
        }

        resetGeometryCaches()
        invalidate()
    }

    fun setBackgroundColorInside(color: Int) {
        setBackgroundColorInsideInternal(color)
        invalidate()
    }

    fun setBackgroundImage(@DrawableRes resId: Int) {
        setBackgroundImageInternal(resId)
        invalidate()
    }

    fun setColors(vararg c: Int) {
        setColorsInternal(c)
        invalidate()
    }

    fun setSpeedMs(ms: Long) {
        val v = ms.coerceAtLeast(100L)
        duration = v
        animator.duration = v
        lastProgress = 0f
        patternPhase = 0f
    }

    fun setAnimationEnabled(enabled: Boolean) {
        if (animationEnabled == enabled) return
        animationEnabled = enabled
        if (isViewAttached && enabled) {
            resetAnimationState()
            animator.start()
        } else {
            animator.cancel()
            resetAnimationState()
            invalidate()
        }
    }

    fun setEdgePatternVector(
        @DrawableRes vectorResId: Int,
        iconSizePx: Float = 14f.dpToPx,
        gapPx: Float = (26f.dpToPx - 14f.dpToPx),
        rotate: Boolean = true,
        phaseMultiplier: Float = 1f,
        evenSpacing: Boolean = true
    ) {
        setEdgePatternVectorInternal(
            vectorResId = vectorResId,
            iconSizePx = iconSizePx,
            rotate = rotate,
            phaseMultiplier = phaseMultiplier,
            evenSpacing = evenSpacing,
            gapPx = gapPx
        )
        invalidate()
    }

    fun setPatternEnabled(enabled: Boolean) {
        setPatternEnabledInternal(enabled)
        invalidate()
    }

    fun applyPreset(preset: EdgePreset, resetState: Boolean = true) {
        val hasBgBefore = backgroundInside != null

        when (preset) {
            is EdgePreset.BackgroundColor -> {
                if (hasBgBefore) setBackgroundColorInsideInternal(preset.color)
                applyEdgeStyleInternal(preset.edge)
            }
            is EdgePreset.BackgroundImageRes -> {
                if (hasBgBefore) setBackgroundImageInternal(preset.resId)
                applyEdgeStyleInternal(preset.edge)
            }
        }

        if (resetState) resetAnimationState()
        invalidate()
    }

    fun setSizePx(px: Float) {
        val v = px.coerceAtLeast(0.5f)
        setPatternIconSizeOnlyPx(v)
        setLinearStrokeWidthOnlyPx(v)
    }

    fun setLinearStrokeWidthPx(px: Float) = setLinearStrokeWidthOnlyPx(px)
    fun setPatternIconSizePx(px: Float) = setPatternIconSizeOnlyPx(px)

    fun setPatternGapPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (patternGapPx == v) return
        patternGapPx = v
        patternAdvance = patternSizePx + patternGapPx
        resetGeometryCaches()
        invalidate()
    }

    fun setTopRadiusPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (topRadius == v) return
        topRadius = v
        resetGeometryCaches()
        invalidate()
    }

    fun setBottomRadiusPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (bottomRadius == v) return
        bottomRadius = v
        resetGeometryCaches()
        invalidate()
    }

    fun getSpeedMs(): Long = duration
    fun getLinearStrokeWidthPx(): Float = strokeWidth
    fun getPatternIconSizePx(): Float = patternSizePx
    fun getSizePx(): Float = if (patternEnabled) patternSizePx else strokeWidth
    fun getTopRadiusPx(): Float = topRadius
    fun getBottomRadiusPx(): Float = bottomRadius
    fun hasBackgroundInside(): Boolean = backgroundInside != null

    fun setAdvanced(value: Advanced, resetState: Boolean = true) {
        when (value) {
            Advanced.DIRECTION_CLOCKWISE,
            Advanced.DIRECTION_ANTI_CLOCKWISE,
            Advanced.DIRECTION_TOP_RIGHT_BOTTOM_LEFT,
            Advanced.DIRECTION_BOTTOM_LEFT_TOP_RIGHT,
            Advanced.DIRECTION_DOWN,
            Advanced.DIRECTION_UP -> setDirection(value, resetState)

            Advanced.NOTCH_DEFAULT,
            Advanced.NOTCH_DISPLAY_NOTCH,
            Advanced.NOTCH_DISPLAY_HOLE,
            Advanced.NOTCH_DISPLAY_INFINITY -> setNotchType(value, resetState)
        }
    }

    fun setDirection(value: Advanced, resetState: Boolean = true) {
        if (value !in listOf(
                Advanced.DIRECTION_CLOCKWISE,
                Advanced.DIRECTION_ANTI_CLOCKWISE,
                Advanced.DIRECTION_TOP_RIGHT_BOTTOM_LEFT,
                Advanced.DIRECTION_BOTTOM_LEFT_TOP_RIGHT,
                Advanced.DIRECTION_DOWN,
                Advanced.DIRECTION_UP
            )
        ) return

        if (direction == value) return
        direction = value

        cachedShader = null

        if (resetState) resetAnimationState()
        invalidate()
    }

    fun getDirection(): Advanced = direction

    fun setNotchType(value: Advanced, resetState: Boolean = true) {
        if (value !in listOf(
                Advanced.NOTCH_DEFAULT,
                Advanced.NOTCH_DISPLAY_NOTCH,
                Advanced.NOTCH_DISPLAY_HOLE,
                Advanced.NOTCH_DISPLAY_INFINITY
            )
        ) return

        if (notchType == value) return
        notchType = value
        resetGeometryCaches()
        if (resetState) resetAnimationState()
        invalidate()
    }

    fun getNotchType(): Advanced = notchType

    fun setNotchWidthPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (notchWidthPx == v) return
        notchWidthPx = v
        resetGeometryCaches()
        invalidate()
    }

    fun setNotchWidthFraction(fraction: Float) {
        val v = fraction.coerceIn(0.05f, 0.95f)
        if (notchWidthFraction == v) return
        notchWidthFraction = v
        resetGeometryCaches()
        invalidate()
    }

    fun getNotchWidthPx(): Float = notchWidthPx
    fun getNotchWidthFraction(): Float = notchWidthFraction

    fun setNotchHeightPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (notchHeightPx == v) return
        notchHeightPx = v
        resetGeometryCaches()
        invalidate()
    }

    fun getNotchHeightPx(): Float = notchHeightPx

    fun setNotchTopRadiusPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (notchTopRadiusPx == v) return
        notchTopRadiusPx = v
        resetGeometryCaches()
        invalidate()
    }

    fun getNotchTopRadiusPx(): Float = notchTopRadiusPx

    fun setNotchBottomRadiusPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (notchBottomRadiusPx == v) return
        notchBottomRadiusPx = v
        resetGeometryCaches()
        invalidate()
    }

    fun getNotchBottomRadiusPx(): Float = notchBottomRadiusPx

    fun setNotchBottomFullness(value: Float) {
        val v = value.coerceIn(0f, 1f)
        if (notchBottomFullness == v) return
        notchBottomFullness = v
        resetGeometryCaches()
        invalidate()
    }

    fun getNotchBottomFullness(): Float = notchBottomFullness

    fun setNotchBottomCapsuleBias(value: Float) {
        val v = value.coerceIn(0f, 1f)
        if (notchBottomCapsuleBias == v) return
        notchBottomCapsuleBias = v
        resetGeometryCaches()
        invalidate()
    }

    fun setHoleShape(shape: EdgeHoleShape) {
        if (holeShape == shape) return
        holeShape = shape
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidate()
    }

    fun getHoleShape(): EdgeHoleShape = holeShape

    fun setHoleOffsetX(px: Float) {
        if (holeOffsetX == px) return
        holeOffsetX = px
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidate()
    }

    fun setHoleOffsetY(px: Float) {
        if (holeOffsetY == px) return
        holeOffsetY = px
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidate()
    }

    fun getHoleOffsetX(): Float = holeOffsetX
    fun getHoleOffsetY(): Float = holeOffsetY

    fun setHoleCircleRadiusPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (holeRadius == v) return
        holeRadius = v
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidate()
    }

    fun getHoleCircleRadiusPx(): Float = holeRadius

    fun setHoleRoundWidthPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (holeWidthPx == v) return
        holeWidthPx = v
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidate()
    }

    fun setHoleRoundHeightPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (holeHeightPx == v) return
        holeHeightPx = v
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidate()
    }

    fun setHoleRoundCornerRadiusPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (holeCornerRadiusPx == v) return
        holeCornerRadiusPx = v
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidate()
    }

    fun getHoleRoundWidthPx(): Float = holeWidthPx
    fun getHoleRoundHeightPx(): Float = holeHeightPx
    fun getHoleRoundCornerRadiusPx(): Float = holeCornerRadiusPx

    fun setInfinityShape(shape: InfinityShape, reset: Boolean = false) {
        if (infinityShape == shape) return
        infinityShape = shape
        resetGeometryCaches()
        if (reset) resetAnimationState()
        invalidate()
    }

    fun getInfinityShape(): InfinityShape = infinityShape

    fun setInfinityWidthPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (infinityWidthPx == v) return
        infinityWidthPx = v
        resetGeometryCaches()
        invalidate()
    }

    fun setInfinityHeightPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (infinityHeightPx == v) return
        infinityHeightPx = v
        resetGeometryCaches()
        invalidate()
    }

    fun setInfinityRadiusTopPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (infinityRadiusTopPx == v) return
        infinityRadiusTopPx = v
        resetGeometryCaches()
        invalidate()
    }

    fun setInfinityRadiusBottomPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (infinityRadiusBottomPx == v) return
        infinityRadiusBottomPx = v
        resetGeometryCaches()
        invalidate()
    }

    fun getInfinityWidthPx(): Float = infinityWidthPx
    fun getInfinityHeightPx(): Float = infinityHeightPx
    fun getInfinityRadiusTopPx(): Float = infinityRadiusTopPx
    fun getInfinityRadiusBottomPx(): Float = infinityRadiusBottomPx
}
