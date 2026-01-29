package com.merryblue.baseapplication.helpers

import android.app.Activity
import android.app.Dialog
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window.FEATURE_NO_TITLE
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.merryblue.baseapplication.domain.repository.TargetSize
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

fun Context.getFullScreenTargetSize(): TargetSize {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val b = wm.currentWindowMetrics.bounds
    return TargetSize(b.width(), b.height())
}

fun Context.clearWallpaperSafely() {
    val wm = WallpaperManager.getInstance(this)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        runCatching { wm.clear(WallpaperManager.FLAG_SYSTEM) }
        runCatching { wm.clear(WallpaperManager.FLAG_LOCK) }
    } else {
        runCatching { wm.clear() }
    }
}

fun Context.restoreBuiltInToSystemAndLock() {
    val wm = WallpaperManager.getInstance(this)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        runCatching {
            wm.getBuiltInDrawable(WallpaperManager.FLAG_SYSTEM)?.let { d ->
                val bmp = (d as? BitmapDrawable)?.bitmap
                if (bmp != null) wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM)
            }
        }
        runCatching {
            wm.getBuiltInDrawable(WallpaperManager.FLAG_LOCK)?.let { d ->
                val bmp = (d as? BitmapDrawable)?.bitmap
                if (bmp != null) wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
            }
        }
    } else {
        runCatching {
            wm.builtInDrawable?.let { d ->
                val bmp = (d as? BitmapDrawable)?.bitmap
                if (bmp != null) wm.setBitmap(bmp)
            }
        }
    }
}

fun Context.createDialog() = Dialog(this).apply {
    requestWindowFeature(FEATURE_NO_TITLE)
    create()
}

fun Dialog.showFullScreen() {
    val window = this.window ?: return
    val finishing = (this.context as? Activity)?.isFinishing ?: false
    if (finishing) return
    this.show()
    window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    window.setLayout(MATCH_PARENT, MATCH_PARENT)
    window.setGravity(CENTER)
}

