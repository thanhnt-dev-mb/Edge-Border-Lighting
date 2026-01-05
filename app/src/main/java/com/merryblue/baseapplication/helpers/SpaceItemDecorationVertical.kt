package com.merryblue.baseapplication.helpers

import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import android.view.View

class SpaceItemDecorationVertical(top: Int) : RecyclerView.ItemDecoration() {
    private val top: Int = top

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.top = top
    }
}
