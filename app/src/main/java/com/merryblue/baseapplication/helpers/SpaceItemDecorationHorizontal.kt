package com.merryblue.baseapplication.helpers

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpaceItemDecorationHorizontal(left: Int) : RecyclerView.ItemDecoration() {

    private val left: Int = left

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = left
    }
}
