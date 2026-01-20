package com.merryblue.baseapplication.ui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import com.merryblue.baseapplication.ui.home.EdgeLightingState
import com.merryblue.baseapplication.ui.view.edgelight.EdgeLightingView

object EdgeWallpaperSnapshot {

    fun render(ctx: Context, width: Int, height: Int, state: EdgeLightingState): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val v = EdgeLightingView(ctx).apply {
            setAnimationEnabled(false)
            applyEdgeState(state)
            layout(0, 0, width, height)
        }

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        v.draw(canvas)

        return bmp
    }
}