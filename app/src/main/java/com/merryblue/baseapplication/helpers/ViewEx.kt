package com.merryblue.baseapplication.helpers

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.max

fun ViewPager2.updateHeightForCurrentPage(extraBottomPx: Int = 0) {
    val rv = getChildAt(0) as? RecyclerView ?: return
    val lm = rv.layoutManager ?: return

    val page = lm.findViewByPosition(currentItem) ?: return

    val wSpec = View.MeasureSpec.makeMeasureSpec(measuredWidth, View.MeasureSpec.EXACTLY)
    val hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    page.measure(wSpec, hSpec)

    val targetH = max(1, page.measuredHeight + extraBottomPx)

    if (layoutParams.height != targetH) {
        layoutParams = layoutParams.apply { height = targetH }
        requestLayout()
    }
}