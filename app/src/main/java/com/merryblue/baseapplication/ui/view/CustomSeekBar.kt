package com.merryblue.baseapplication.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.use
import androidx.core.view.ViewCompat
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.helpers.dpToPx
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CustomSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0.65f
    private var enabledTouch = true

    private var paddingHorizontalPx = 24f.dpToPx
    private var inactiveThicknessPx = 4f.dpToPx
    private var activeThicknessPx = 7f.dpToPx

    private var thumbRadiusPx = 9f.dpToPx
    private var thumbShadowDyPx = 2f.dpToPx

    private var progressColor = Color.parseColor("#EF7979")
    private var trackColor = Color.parseColor("#FFCCCC")
    private var thumbColor = Color.parseColor("#EF7979")
    private var thumbShadowColor = Color.parseColor("#40000000")

    private var listener: OnProgressChangeListener? = null

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isDither = true
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isDither = true
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
    }

    private val thumbShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
    }

    private var centerY = 0f
    private var startX = 0f
    private var endX = 0f
    private var denom = 1f
    private var progressX = 0f

    private val dirtyRect = Rect()
    private val maxThicknessPx: Float get() = max(inactiveThicknessPx, activeThicknessPx)

    private val EPS = 1e-4f

    init {
        parseAttrs(attrs)
        syncPaints()
    }

    private fun parseAttrs(attrs: AttributeSet?) {
        if (attrs == null) return
        context.obtainStyledAttributes(attrs, R.styleable.CustomSeekBar).use { a ->
            progress = a.getFloat(R.styleable.CustomSeekBar_csb_progress, progress).coerceIn(0f, 1f)
            enabledTouch = a.getBoolean(R.styleable.CustomSeekBar_csb_enabledTouch, enabledTouch)
            paddingHorizontalPx = a.getDimension(
                R.styleable.CustomSeekBar_csb_paddingHorizontal,
                paddingHorizontalPx
            )
            inactiveThicknessPx = a.getDimension(
                R.styleable.CustomSeekBar_csb_trackThicknessInactive,
                inactiveThicknessPx
            )
            activeThicknessPx = a.getDimension(
                R.styleable.CustomSeekBar_csb_trackThicknessActive,
                activeThicknessPx
            )
            progressColor = a.getColor(R.styleable.CustomSeekBar_csb_progressColor, progressColor)
            trackColor = a.getColor(R.styleable.CustomSeekBar_csb_trackColor, trackColor)
            thumbColor = a.getColor(R.styleable.CustomSeekBar_csb_thumbColor, thumbColor)
            thumbRadiusPx = a.getDimension(R.styleable.CustomSeekBar_csb_thumbRadius, thumbRadiusPx)
            thumbShadowColor = a.getColor(R.styleable.CustomSeekBar_csb_thumbShadowColor, thumbShadowColor)
            thumbShadowDyPx = a.getDimension(R.styleable.CustomSeekBar_csb_thumbShadowDy, thumbShadowDyPx)
        }
    }

    private fun syncPaints() {
        trackPaint.color = trackColor
        trackPaint.strokeWidth = inactiveThicknessPx

        progressPaint.color = progressColor
        progressPaint.strokeWidth = activeThicknessPx

        thumbPaint.color = thumbColor
        thumbShadowPaint.color = thumbShadowColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeGeometry()
    }

    private fun recomputeGeometry() {
        centerY = height / 2f
        startX = paddingHorizontalPx
        endX = width - paddingHorizontalPx
        denom = max(1f, endX - startX)
        progressX = startX + denom * progress
        // update dirty rect bounds (for partial invalidation)
        updateDirtyRect()
    }

    private fun updateProgressX() {
        progressX = startX + denom * progress
        updateDirtyRect()
    }

    private fun updateDirtyRect() {
        val extraY = max(thumbRadiusPx + abs(thumbShadowDyPx), maxThicknessPx / 2f) + 2f
        val left = (min(startX, endX) - 2f).roundToInt()
        val right = (max(startX, endX) + 2f).roundToInt()
        val top = (centerY - extraY).roundToInt()
        val bottom = (centerY + extraY).roundToInt()
        dirtyRect.set(left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawLine(startX, centerY, endX, centerY, trackPaint)
        canvas.drawLine(startX, centerY, progressX, centerY, progressPaint)

        canvas.drawCircle(progressX, centerY + thumbShadowDyPx, thumbRadiusPx, thumbShadowPaint)
        canvas.drawCircle(progressX, centerY, thumbRadiusPx, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enabledTouch) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onStartTrackingTouch(this)
                updateProgressFromX(event.x, fromUser = true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                updateProgressFromX(event.x, fromUser = true)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                updateProgressFromX(event.x, fromUser = true)
                listener?.onStopTrackingTouch(this)
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateProgressFromX(x: Float, fromUser: Boolean) {
        val clampedX = x.coerceIn(startX, endX)
        val newValue = ((clampedX - startX) / denom).coerceIn(0f, 1f)
        setProgressInternal(newValue, fromUser)
    }

    private fun setProgressInternal(value: Float, fromUser: Boolean) {
        val v = value.coerceIn(0f, 1f)
        if (abs(v - progress) < EPS) return

        progress = v
        updateProgressX()

        listener?.onProgressChanged(this, progress, fromUser)

        ViewCompat.postInvalidateOnAnimation(this, dirtyRect.left, dirtyRect.top, dirtyRect.right, dirtyRect.bottom)
    }

    fun setProgress(value: Float) = setProgressInternal(value, fromUser = false)
    fun getProgress(): Float = progress

    fun setOnProgressChangeListener(listener: OnProgressChangeListener?) {
        this.listener = listener
    }

    fun progress(value: Float) = apply { setProgressInternal(value, fromUser = false) }

    fun enabledTouch(enabled: Boolean) = apply { enabledTouch = enabled }

    fun paddingHorizontalPx(px: Float) = apply {
        paddingHorizontalPx = px
        recomputeGeometry()
        invalidate()
    }

    fun paddingHorizontalDp(dp: Float) = apply {
        paddingHorizontalPx = dp.dpToPx
        recomputeGeometry()
        invalidate()
    }

    fun trackThicknessPx(inactivePx: Float, activePx: Float) = apply {
        inactiveThicknessPx = inactivePx
        activeThicknessPx = activePx
        syncPaints()
        updateDirtyRect()
        invalidate()
    }

    fun trackThicknessDp(inactiveDp: Float, activeDp: Float) = apply {
        trackThicknessPx(inactiveDp.dpToPx, activeDp.dpToPx)
    }

    fun colors(progressColor: Int, trackColor: Int) = apply {
        this.progressColor = progressColor
        this.trackColor = trackColor
        syncPaints()
        invalidate()
    }

    fun thumbColor(color: Int) = apply {
        thumbColor = color
        syncPaints()
        invalidate()
    }

    fun thumbRadiusPx(px: Float) = apply {
        thumbRadiusPx = px
        updateDirtyRect()
        invalidate()
    }

    fun thumbRadiusDp(dp: Float) = apply {
        thumbRadiusPx = dp.dpToPx
        updateDirtyRect()
        invalidate()
    }

    fun thumbShadow(color: Int, dyPx: Float = thumbShadowDyPx) = apply {
        thumbShadowColor = color
        thumbShadowDyPx = dyPx
        syncPaints()
        updateDirtyRect()
        invalidate()
    }

    fun thumbShadowDp(color: Int, dyDp: Float) = apply {
        thumbShadow(color, dyDp.dpToPx)
    }

    interface OnProgressChangeListener {
        fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean)
        fun onStartTrackingTouch(seekBar: CustomSeekBar) {}
        fun onStopTrackingTouch(seekBar: CustomSeekBar) {}
    }
}
