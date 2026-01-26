package com.merryblue.baseapplication.ui.view.edgelight

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.util.Xml
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
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
import androidx.core.graphics.withTranslation
import com.merryblue.baseapplication.helpers.EdgeStyle.EDGE_LINEAR
import com.merryblue.baseapplication.helpers.EdgeStyle.EDGE_PATTERN
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeBackground
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeHoleShape
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeImageScaleType
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeLightingState
import com.merryblue.baseapplication.ui.view.edgelight.model.HoleCenterBounds
import com.merryblue.baseapplication.ui.view.edgelight.model.InfinityParams
import com.merryblue.baseapplication.ui.view.edgelight.model.InfinityShape
import com.merryblue.baseapplication.ui.view.edgelight.model.OffsetRange
import com.merryblue.baseapplication.ui.view.edgelight.model.VectorPathResult

class EdgeLightingView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var edgeEnabled = true
    private var backgroundEnabled = true

    private var batchDepth = 0
    private var pendingInvalidateSmart = false

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
    private var infinitySideCurvature: Float = -1f
    private var infinityTipRadiusPx: Float = 0f
    private var infinityWidthPx: Float = -1f
    private var infinityHeightPx: Float = 20f.dpToPx
    private var infinityRadiusTopPx: Float = -1f
    private var infinityRadiusBottomPx: Float = -1f
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
            invalidateSmart()
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

    private fun batch(block: () -> Unit) {
        batchDepth++
        try {
            block()
        } finally {
            batchDepth--
            if (batchDepth == 0) {
                if (pendingInvalidateSmart) {
                    pendingInvalidateSmart = false
                    postInvalidateOnAnimation()
                }
            }
        }
    }

    private fun invalidateSmart() {
        if (batchDepth > 0) {
            pendingInvalidateSmart = true
        } else {
            postInvalidateOnAnimation()
        }
    }

    private fun resetGeometryCaches() {
        cachedBgPath = null
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

        if (backgroundEnabled) drawBackground(canvas, bgPath)
        if (edgeEnabled) drawEdge(canvas, edgePath)
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

            // Optional infinity attrs if you have them in styleable (keep safe defaults if missing)
            // infinityWidthPx = it.getDimension(R.styleable.EdgeLightingView_edgeInfinityWidth, infinityWidthPx)
            // infinityHeightPx = it.getDimension(R.styleable.EdgeLightingView_edgeInfinityHeight, infinityHeightPx)
            // infinityRadiusTopPx = it.getDimension(R.styleable.EdgeLightingView_edgeInfinityRadiusTop, infinityRadiusTopPx)

            val colorsRes = it.getResourceId(R.styleable.EdgeLightingView_edgeColors, 0)
            if (colorsRes != 0) colors = resources.getIntArray(colorsRes)

            val bgColor = it.getColor(R.styleable.EdgeLightingView_edgeBackgroundColor, Color.TRANSPARENT)
            if (bgColor != Color.TRANSPARENT) setBackgroundColorInside(bgColor)

            val bgImageRes = it.getResourceId(R.styleable.EdgeLightingView_edgeBackgroundImage, 0)
            if (bgImageRes != 0) setBackgroundImageResId(bgImageRes)
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

        fun addRoundRectTopBottom(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            topR: Float,
            bottomR: Float,
        ) {
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

                val topMid = width / 2f
                val topR = tr.coerceAtMost((r - pad) / 2f)
                val botR = br.coerceAtMost((b - pad) / 2f)

                path.reset()

                path.moveTo(pad + botR, b)
                path.quadTo(pad, b, pad, b - botR)
                path.lineTo(pad, pad + topR)
                path.quadTo(pad, pad, pad + topR, pad)

                val p = resolveInfinityParams(inset = pad, outerTopRadius = topR, outerBottomRadius = botR)
                val infW = p.w
                val infH = p.h
                val rTop = p.rTop

                val halfW = infW / 2f
                val startX = (topMid - halfW).coerceAtLeast(pad + topR)
                val endX = (topMid + halfW).coerceAtMost(r - topR)

                path.lineTo(startX, pad)

                when (infinityShape) {
                    InfinityShape.U -> buildInfinityUPath(
                        out = path,
                        startX = startX,
                        endX = endX,
                        topY = pad,
                        bottomY = pad + infH,
                        radiusTop = rTop
                    )

                    InfinityShape.V -> buildInfinityVPath(
                        out = path,
                        startX = startX,
                        endX = endX,
                        topY = pad,
                        bottomY = pad + infH,
                        radiusTop = rTop,
                        tipRadius = infinityTipRadiusPx,
                    )
                }

                path.lineTo(r - topR, pad)
                path.quadTo(r, pad, r, pad + topR)

                path.lineTo(r, b - botR)
                path.quadTo(r, b, r - botR, b)

                path.lineTo(pad + botR, b)
                path.close()
            }

            else -> Unit
        }

        return path
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

        val right = width - inset
        val bottom = height - inset
        val topMid = width / 2f

        val usableW = (right - inset).coerceAtLeast(1f)
        val topR = (topRadius - iconHalf).coerceAtLeast(0f).coerceAtMost(usableW / 2f)
        val botR = (bottomRadius - iconHalf).coerceAtLeast(0f).coerceAtMost((bottom - inset) / 2f)

        val p = resolveInfinityParams(inset = inset, outerTopRadius = topR, outerBottomRadius = botR)
        val infW = p.w
        val infH = p.h
        val rTop = p.rTop

        val halfW = infW / 2f
        val startX = (topMid - halfW).coerceAtLeast(inset + topR)
        val endX = (topMid + halfW).coerceAtMost(right - topR)

        path.moveTo(inset + botR, bottom)
        path.quadTo(inset, bottom, inset, bottom - botR)
        path.lineTo(inset, inset + topR)
        path.quadTo(inset, inset, inset + topR, inset)

        path.lineTo(startX, inset)

        when (infinityShape) {
            InfinityShape.U -> buildInfinityUPath(path, startX, endX, inset, inset + infH, rTop)
            InfinityShape.V -> buildInfinityVPath(
                path,
                startX,
                endX,
                inset,
                inset + infH,
                rTop,
                tipRadius = infinityTipRadiusPx
            )
        }

        path.lineTo(right - topR, inset)
        path.quadTo(right, inset, right, inset + topR)
        path.lineTo(right, bottom - botR)
        path.quadTo(right, bottom, right - botR, bottom)
        path.lineTo(inset + botR, bottom)
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
        notchBottomFullness: Float,
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
                val needRecreate = (cachedBitmap != bg.bitmap) ||
                        (cachedBgW != width) ||
                        (cachedBgH != height) ||
                        (cachedBitmapShader == null)

                if (needRecreate) {

                    cachedBitmap = bg.bitmap
                    cachedBgW = width
                    cachedBgH = height
                    cachedBitmapShader = BitmapShader(bg.bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

                    if (width > 0 && height > 0) {
                        updateBgMatrix(width.toFloat(), height.toFloat())
                        cachedBitmapShader?.setLocalMatrix(bgMatrix)
                    }

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
            canvas.withTranslation(pos[0], pos[1]) {
                if (patternRotate) {
                    val angle = atan2(tan[1], tan[0]) * 180f / Math.PI.toFloat()
                    rotate(angle)
                }
                drawPath(icon, edgePaint)
            }
        }
    }

    private fun signedPhase(phase: Float): Float {
        return when (direction) {
            Advanced.DIRECTION_ANTI_CLOCKWISE,
            Advanced.DIRECTION_UP,
            Advanced.DIRECTION_TOP_RIGHT_BOTTOM_LEFT, -> -phase
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

    @SuppressLint("ResourceType")
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
        outerBottomRadius: Float,
    ): InfinityParams {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)

        val maxAvailableW = (w - 2f * inset - 2f * outerTopRadius).coerceAtLeast(10f)
        val autoW = (w * 0.42f).coerceIn(60f.dpToPx, maxAvailableW)
        val infW = (if (infinityWidthPx >= 0f) infinityWidthPx else autoW).coerceIn(10f, maxAvailableW)

        val maxH = (h - inset - outerBottomRadius - inset - 1f).coerceAtLeast(0f)
        val autoH = 20f.dpToPx.coerceAtMost(maxH)
        val infH = (if (infinityHeightPx >= 0f) infinityHeightPx else autoH).coerceIn(0f, maxH)

        val halfW = infW / 2f
        val rTopRaw = if (infinityRadiusTopPx >= 0f) infinityRadiusTopPx else outerTopRadius.coerceAtLeast(0f)
        val rTop = rTopRaw.coerceIn(0f, min(halfW, infH))

        val rBotRaw = if (infinityRadiusBottomPx >= 0f) infinityRadiusBottomPx else 0f
        val rBot = rBotRaw.coerceIn(0f, min(halfW, infH))

        return InfinityParams(infW, infH, rTop, rBot)
    }

    private fun buildInfinityUPath(
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

        val c1y = topY + 0.85f * h
        val c2x = cx - 0.25f * w

        out.cubicTo(
            leftJoinX, c1y,
            c2x, bottomY,
            cx, bottomY
        )

        val c3x = cx + 0.25f * w
        val c4y = topY + 0.85f * h

        out.cubicTo(
            c3x, bottomY,
            rightJoinX, c4y,
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
        tipRadius: Float = 2f.dpToPx,
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
        val mouthY = topY + rTop

        val mouthLx = startX + rTop
        val mouthRx = endX - rTop

        val usableH = (bottomY - mouthY).coerceAtLeast(0f)

        val c = resolveSideCurvature(rTop)

        val maxInX = (w / 2f - rTop).coerceAtLeast(0f)
        val inX = maxInX * (0.15f + 0.65f * c)

        val c1y = mouthY + usableH * (0.45f + 0.20f * c)
        val c2y = bottomY - usableH * (0.15f + 0.10f * c)

        val maxTipByH = h * 0.45f
        val maxTipByW = (mouthRx - mouthLx) * 0.45f
        val tipR = tipRadius.coerceIn(0f, minOf(maxTipByH, maxTipByW))

        if (rTop > 0.5f) {
            out.cubicTo(startX + k * rTop, topY, startX + rTop, topY + (1f - k) * rTop, mouthLx, mouthY)
        } else {
            out.lineTo(startX, topY)
            out.lineTo(mouthLx, mouthY)
        }

        if (tipR <= 0.5f) {
            out.cubicTo(mouthLx, c1y, cx - inX, c2y, cx, bottomY)
            out.cubicTo(cx + inX, c2y, mouthRx, c1y, mouthRx, mouthY)
        } else {
            val lx = cx - mouthLx
            val ly = bottomY - mouthY
            val leftLen = kotlin.math.sqrt(lx * lx + ly * ly).coerceAtLeast(1f)

            val rx = cx - mouthRx
            val ry = bottomY - mouthY
            val rightLen = kotlin.math.sqrt(rx * rx + ry * ry).coerceAtLeast(1f)

            val leftNearX  = cx - (lx / leftLen) * tipR
            val leftNearY  = bottomY - (ly / leftLen) * tipR
            val rightNearX = cx - (rx / rightLen) * tipR
            val rightNearY = bottomY - (ry / rightLen) * tipR

            out.cubicTo(mouthLx, c1y, cx - inX, c2y, leftNearX, leftNearY)
            out.quadTo(cx, bottomY, rightNearX, rightNearY)
            out.cubicTo(cx + inX, c2y, mouthRx, c1y, mouthRx, mouthY)
        }

        if (rTop > 0.5f) {
            out.cubicTo(endX - rTop, topY + k * rTop, endX - k * rTop, topY, endX, topY)
        } else {
            out.lineTo(endX, topY)
        }
    }

    private fun setBackgroundColorInsideInternal(color: Int) {
        backgroundInside = EdgeBackground.Color(color)
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
        gapPx: Float,
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
        if (strokeWidth == px) return
        strokeWidth = px
        edgePaint.strokeWidth = px
        resetGeometryCaches()
        invalidateSmart()
    }

    private fun setPatternIconSizeOnlyPx(iconSizePx: Float) {
        if (patternSizePx == iconSizePx) return
        patternSizePx = iconSizePx
        patternAdvance = patternSizePx + patternGapPx

        if (patternVectorResId != 0) {
            val vp = loadVectorAsPath(context, patternVectorResId)
            patternPath = vp.path.centerAndScaleTo(patternSizePx, vp.viewportW, vp.viewportH)
        }

        resetGeometryCaches()
        invalidateSmart()
    }

    fun setBackgroundColorInside(color: Int) {
        setBackgroundColorInsideInternal(color)
        invalidateSmart()
    }

    fun setBackgroundImageResId(@DrawableRes resId: Int) {
        val bmp = BitmapFactory.decodeResource(resources, resId)
        setBackgroundBitmap(bmp)
    }

    fun setBackgroundFromFilePath(path: String) {
        clearBackgroundBitmap()
        val bmp = BitmapFactory.decodeFile(path) ?: throw IllegalStateException("Decode bitmap failed: $path")
        backgroundInside = EdgeBackground.Image(bmp)
        cachedBitmap = null
        cachedBitmapShader = null
        cachedBgW = 0
        cachedBgH = 0
        invalidateSmart()
    }

    @MainThread
    fun setBackgroundBitmap(bitmap: Bitmap) {
        clearBackgroundBitmap()
        backgroundInside = EdgeBackground.Image(bitmap)
        cachedBitmap = null
        cachedBitmapShader = null
        cachedBgW = 0
        cachedBgH = 0
        invalidateSmart()
    }

    @MainThread
    fun clearBackgroundBitmap() {
        backgroundInside = EdgeBackground.Color(Color.TRANSPARENT)
        cachedBitmap = null
        cachedBitmapShader = null
        cachedBgW = 0
        cachedBgH = 0
        invalidateSmart()
    }

    fun setColors(vararg c: Int) {
        setColorsInternal(c)
        invalidateSmart()
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
            invalidateSmart()
        }
    }

    fun setEdgePatternVector(
        @DrawableRes vectorResId: Int,
        iconSizePx: Float = 14f.dpToPx,
        gapPx: Float = (26f.dpToPx - 14f.dpToPx),
        rotate: Boolean = true,
        phaseMultiplier: Float = 1f,
        evenSpacing: Boolean = true,
    ) {
        setEdgePatternVectorInternal(
            vectorResId = vectorResId,
            iconSizePx = iconSizePx,
            rotate = rotate,
            phaseMultiplier = phaseMultiplier,
            evenSpacing = evenSpacing,
            gapPx = gapPx
        )
        invalidateSmart()
    }

    fun setPatternEnabled(enabled: Boolean) {
        setPatternEnabledInternal(enabled)
        invalidateSmart()
    }

    fun applyPreset(preset: EdgePreset, resetState: Boolean = true) {
        val hasBgBefore = backgroundInside != null

        when (preset) {
            is EdgePreset.BackgroundColor -> {
                if (hasBgBefore) setBackgroundColorInsideInternal(preset.color)
                applyEdgeStyleInternal(preset.edge)
            }
            is EdgePreset.BackgroundImageRes -> {
                if (hasBgBefore) {
                    val bmp = BitmapFactory.decodeResource(resources, preset.resId)
                    setBackgroundBitmap(bmp)
                }
                applyEdgeStyleInternal(preset.edge)
            }
        }

        if (resetState) resetAnimationState()
        invalidateSmart()
    }

    fun setSizePx(px: Float) {
        setPatternIconSizeOnlyPx(px)
        setLinearStrokeWidthOnlyPx(px)
    }

    fun setLinearStrokeWidthPx(px: Float) = setLinearStrokeWidthOnlyPx(px)
    fun setPatternIconSizePx(px: Float) = setPatternIconSizeOnlyPx(px)

    fun setPatternGapPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (patternGapPx == v) return
        patternGapPx = v
        patternAdvance = patternSizePx + patternGapPx
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setTopRadiusPx(px: Float) {
        if (topRadius == px) return
        topRadius = px
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setBottomRadiusPx(px: Float) {
        if (bottomRadius == px) return
        bottomRadius = px
        resetGeometryCaches()
        invalidateSmart()
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
            Advanced.DIRECTION_UP,
                -> setDirection(value, resetState)

            Advanced.NOTCH_DEFAULT,
            Advanced.NOTCH_DISPLAY_NOTCH,
            Advanced.NOTCH_DISPLAY_HOLE,
            Advanced.NOTCH_DISPLAY_INFINITY,
                -> setNotchType(value, resetState)
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
        invalidateSmart()
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
        invalidateSmart()
    }

    fun getNotchType(): Advanced = notchType

    fun setNotchWidthPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (notchWidthPx == v) return
        notchWidthPx = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setNotchWidthFraction(fraction: Float) {
        val v = fraction.coerceIn(0.05f, 0.95f)
        if (notchWidthFraction == v) return
        notchWidthFraction = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun getNotchWidthPx(): Float = notchWidthPx
    fun getNotchWidthFraction(): Float = notchWidthFraction

    fun setNotchHeightPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (notchHeightPx == v) return
        notchHeightPx = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun getNotchHeightPx(): Float = notchHeightPx

    fun setNotchTopRadiusPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (notchTopRadiusPx == v) return
        notchTopRadiusPx = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun getNotchTopRadiusPx(): Float = notchTopRadiusPx

    fun setNotchBottomRadiusPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (notchBottomRadiusPx == v) return
        notchBottomRadiusPx = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun getNotchBottomRadiusPx(): Float = notchBottomRadiusPx

    fun setNotchBottomFullness(value: Float) {
        val v = value.coerceIn(0f, 1f)
        if (notchBottomFullness == v) return
        notchBottomFullness = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun getNotchBottomFullness(): Float = notchBottomFullness

    fun setNotchBottomCapsuleBias(value: Float) {
        val v = value.coerceIn(0f, 1f)
        if (notchBottomCapsuleBias == v) return
        notchBottomCapsuleBias = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setHoleShape(shape: EdgeHoleShape) {
        if (holeShape == shape) return
        holeShape = shape
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidateSmart()
    }

    fun getHoleShape(): EdgeHoleShape = holeShape

    fun setHoleOffsetX(px: Float) {
        if (holeOffsetX == px) return
        holeOffsetX = px
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setHoleOffsetY(px: Float) {
        if (holeOffsetY == px) return
        holeOffsetY = px
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidateSmart()
    }

    fun getHoleOffsetX(): Float = holeOffsetX
    fun getHoleOffsetY(): Float = holeOffsetY

    fun setHoleCircleRadiusPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (holeRadius == v) return
        holeRadius = v
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidateSmart()
    }

    fun getHoleCircleRadiusPx(): Float = holeRadius

    fun setHoleRoundWidthPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (holeWidthPx == v) return
        holeWidthPx = v
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setHoleRoundHeightPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (holeHeightPx == v) return
        holeHeightPx = v
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setHoleRoundCornerRadiusPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (holeCornerRadiusPx == v) return
        holeCornerRadiusPx = v
        if (notchType == Advanced.NOTCH_DISPLAY_HOLE) clampHoleOffsetsForCurrentConfig()
        resetGeometryCaches()
        invalidateSmart()
    }

    fun getHoleRoundWidthPx(): Float = holeWidthPx
    fun getHoleRoundHeightPx(): Float = holeHeightPx
    fun getHoleRoundCornerRadiusPx(): Float = holeCornerRadiusPx

    fun setInfinityShape(shape: InfinityShape, reset: Boolean = false) {
        if (infinityShape == shape) return
        infinityShape = shape
        resetGeometryCaches()
        if (reset) resetAnimationState()
        invalidateSmart()
    }

    fun getInfinityShape(): InfinityShape = infinityShape

    fun setInfinityWidthPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (infinityWidthPx == v) return
        infinityWidthPx = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setInfinityWidthAuto() {
        if (infinityWidthPx < 0f) return
        infinityWidthPx = -1f
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setInfinityHeightPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (infinityHeightPx == v) return
        infinityHeightPx = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setInfinityHeightAuto() {
        if (infinityHeightPx < 0f) return
        infinityHeightPx = -1f
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setInfinityRadiusTopPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (infinityRadiusTopPx == v) return
        infinityRadiusTopPx = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setInfinityRadiusTopAuto() {
        if (infinityRadiusTopPx < 0f) return
        infinityRadiusTopPx = -1f
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setInfinityTipRadiusPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (infinityTipRadiusPx == v) return
        infinityTipRadiusPx = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun getInfinityTipRadiusPx(): Float = infinityTipRadiusPx

    fun setInfinityRadiusBottomPx(px: Float) {
        val v = px.coerceAtLeast(0f)
        if (infinityRadiusBottomPx == v) return
        infinityRadiusBottomPx = v
        resetGeometryCaches()
        invalidateSmart()
    }

    fun setInfinityRadiusBottomAuto() {
        if (infinityRadiusBottomPx < 0f) return
        infinityRadiusBottomPx = -1f
        resetGeometryCaches()
        invalidateSmart()
    }

    private fun resolveSideCurvature(rTop: Float): Float {
        if (infinitySideCurvature >= 0f) return infinitySideCurvature
        val t = (rTop / (rTop + 30f.dpToPx)).coerceIn(0f, 1f)
        return t
    }

    fun getInfinityWidthPx(): Float = infinityWidthPx
    fun getInfinityHeightPx(): Float = infinityHeightPx
    fun getInfinityRadiusTopPx(): Float = infinityRadiusTopPx
    fun getInfinityRadiusBottomPx(): Float = infinityRadiusBottomPx

    fun applyEdgeState(s: EdgeLightingState) = batch {

        setBackgroundEnabled(s.backgroundEnabled)
        setEdgeEnabled(s.edgeEnabled)

        // speed
        setSpeedMs(s.speedMs)

        // size
        setSizePx(s.iconSizePx)

        // radius
        setTopRadiusPx(s.topRadius)
        setBottomRadiusPx(s.bottomRadius)

        // advanced
        setDirection(s.direction, resetState = false)
        setNotchType(s.notchType, resetState = false)

        // notch params
        setNotchWidthFraction(s.notchWidthFraction)
        setNotchHeightPx(s.notchHeightPx)
        setNotchTopRadiusPx(s.notchTopRadiusPx)
        setNotchBottomRadiusPx(s.notchBottomRadiusPx)
        setNotchBottomFullness(s.notchBottomFullness)

        // hole params
        setHoleShape(s.holeShape)
        setHoleOffsetX(s.holeOffsetX)
        setHoleOffsetY(s.holeOffsetY)
        setHoleCircleRadiusPx(s.holeRadius)
        setHoleRoundWidthPx(s.holeWidthPx)
        setHoleRoundHeightPx(s.holeHeightPx)
        setHoleRoundCornerRadiusPx(s.holeCornerRadiusPx)

        // infinity params
        setInfinityShape(s.infinityShape, reset = false)
        if (s.infinityWidthPx >= 0f) setInfinityWidthPx(s.infinityWidthPx) else setInfinityWidthAuto()
        if (s.infinityHeightPx >= 0f) setInfinityHeightPx(s.infinityHeightPx) else setInfinityHeightAuto()
        if (s.infinityRadiusTopPx >= 0f) setInfinityRadiusTopPx(s.infinityRadiusTopPx) else setInfinityRadiusTopAuto()

        // style
        when (s.edgeStyleType) {
            EDGE_LINEAR -> { // linear
                setPatternEnabled(false)
                setColors(*s.colors)
                setLinearStrokeWidthPx(s.iconSizePx)
            }

            EDGE_PATTERN -> {
                if (s.vectorResId == R.drawable.ic_none || s.vectorResId == 0) {
                    setPatternEnabled(false)
                } else {
                    setEdgePatternVector(
                        vectorResId = s.vectorResId,
                        iconSizePx = s.iconSizePx,
                        gapPx = (s.advancePx - s.iconSizePx).coerceAtLeast(0f),
                        rotate = s.rotate,
                        phaseMultiplier = s.phaseMultiplier,
                        evenSpacing = true
                    )
                }
                setColors(*s.colors)
            }

            else -> {
                setPatternEnabled(false)
                setEdgeEnabled(false)
                setNotchType(Advanced.NOTCH_DEFAULT)
                setSizePx(0f)
                setTopRadiusPx(0f)
                setBottomRadiusPx(0f)
            }
        }
    }

    fun setBackgroundScaleType(type: EdgeImageScaleType) {
        if (imageScaleType == type) return
        imageScaleType = type

        if (cachedBitmap != null) {
            cachedBgW = 0
            cachedBgH = 0
            updateBgMatrix(width.toFloat().coerceAtLeast(1f), height.toFloat().coerceAtLeast(1f))
            cachedBitmapShader?.setLocalMatrix(bgMatrix)
        }
        invalidateSmart()
    }

    fun setEdgeEnabled(enabled: Boolean) {
        if (edgeEnabled == enabled) return
        edgeEnabled = enabled

        if (!enabled) {
            animator.cancel()
            resetAnimationState()
        } else {
            if (isViewAttached && animationEnabled) {
                resetAnimationState()
                animator.start()
            }
        }
        invalidateSmart()
    }

    fun setBackgroundEnabled(enabled: Boolean) {
        if (backgroundEnabled == enabled) return
        backgroundEnabled = enabled
        invalidateSmart()
    }

    fun isEdgeEnabled(): Boolean = edgeEnabled

    fun isBackgroundEnabled(): Boolean = backgroundEnabled

    fun startAnimationForWallpaper() {
        if (!animationEnabled) animationEnabled = true
        resetAnimationState()
        animator.start()
    }

    fun stopAnimationForWallpaper() {
        animator.cancel()
    }
}
