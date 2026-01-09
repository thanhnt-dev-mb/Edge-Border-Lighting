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

class CustomToggle @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs), Checkable {

    private var checked = false
    private lateinit var thumb: View
    private lateinit var track: View

    private var thumbAnimator: ObjectAnimator? = null
    private var listener: ((Boolean) -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        inflate(context, R.layout.view_custom_toggle, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        track = findViewById(R.id.track)
        thumb = findViewById(R.id.thumb)
        updateUI(false)
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
        updateUI(true)
        listener?.invoke(checked)
    }

    private fun updateUI(animate: Boolean) {
        thumb.isSelected = checked
        track.isSelected = checked

        val travel = width - thumb.width - 7f.dpToPx
        val targetX = if (checked) travel else 0f

        if (animate) {
            thumbAnimator?.cancel()
            thumbAnimator = ObjectAnimator.ofFloat(thumb, "translationX", thumb.translationX, targetX)
            thumbAnimator?.apply {
                duration = 180
                interpolator = FastOutSlowInInterpolator()
                start()
            }
        } else {
            thumb.translationX = targetX
        }
    }

    fun setOnCheckedChangeListener(l: (Boolean) -> Unit) {
        listener = l
    }

}
