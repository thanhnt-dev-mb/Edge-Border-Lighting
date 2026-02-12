package com.merryblue.baseapplication.ui.view

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import android.widget.FrameLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.helpers.dpToPx

class CustomToggle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), Checkable {

    private var checked = false
    private lateinit var thumb: View
    private lateinit var track: View

    private var thumbAnimator: ObjectAnimator? = null
    private var listener: ((Boolean) -> Unit)? = null
    private var broadcasting = false

    init {
        isClickable = true
        isFocusable = true
        inflate(context, R.layout.view_custom_toggle, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        track = findViewById(R.id.track)
        thumb = findViewById(R.id.thumb)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) updateThumbPosition(animate = false)
    }

    override fun performClick(): Boolean {
        toggle()
        return super.performClick()
    }

    override fun isChecked(): Boolean = checked

    override fun toggle() {
        setChecked(!checked)
    }

    override fun setChecked(isChecked: Boolean) {
        if (checked == isChecked) return
        checked = isChecked
        updateUI(animate = true)
        notifyListener()
    }

    fun setCheckedSilently(isChecked: Boolean, notify: Boolean = true) {
        if (checked == isChecked) return
        checked = isChecked
        updateUI(animate = false)
        if (notify) notifyListener()
    }

    private fun updateUI(animate: Boolean) {
        thumb.isSelected = checked
        track.isSelected = checked
        updateThumbPosition(animate)
    }

    private fun updateThumbPosition(animate: Boolean) {
        if (width == 0 || thumb.width == 0) {
            post { updateThumbPosition(false) }
            return
        }

        val thumbStartPadding = 3.5f.dpToPx
        val thumbEndPadding = 3.5f.dpToPx
        val travel = width - thumb.width - thumbStartPadding - thumbEndPadding
        val targetX = if (checked) travel else 0f

        if (animate) {
            animateThumbTo(targetX)
        } else {
            thumbAnimator?.cancel()
            thumb.translationX = targetX
        }
    }

    private fun animateThumbTo(targetX: Float) {
        thumbAnimator?.cancel()
        thumbAnimator = ObjectAnimator.ofFloat(thumb, TRANSLATION_X, thumb.translationX, targetX).apply {
            duration = 180L
            interpolator = FastOutSlowInInterpolator()
            start()
        }
    }

    private fun notifyListener() {
        if (broadcasting) return
        broadcasting = true
        try {
            listener?.invoke(checked)
        } finally {
            broadcasting = false
        }
    }

    fun setOnCheckedChangeListener(listener: ((Boolean) -> Unit)?) {
        this.listener = listener
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        thumbAnimator?.cancel()
        thumbAnimator = null
        listener = null
    }
}