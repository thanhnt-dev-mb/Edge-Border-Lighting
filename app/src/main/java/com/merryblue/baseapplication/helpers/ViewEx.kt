package com.merryblue.baseapplication.helpers

import android.app.Activity
import android.app.Dialog
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.Gravity
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import android.view.Window.FEATURE_NO_TITLE
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.domain.repository.TargetSize
import kotlin.math.max

fun BottomSheetDialog.setupFullScreen() {
    setOnShowListener { dialogInterface ->
        val bottomSheetDialog = dialogInterface as BottomSheetDialog
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

        bottomSheetDialog.window?.applyFullScreenMode()
    }
}

fun Window.applyFullScreenMode() {
    WindowCompat.setDecorFitsSystemWindows(this, false)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val controller = WindowCompat.getInsetsController(this, decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
}

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

    val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+
        val b = wm.currentWindowMetrics.bounds
        b.width() to b.height()
    } else {
        // Android 10 trở xuống
        @Suppress("DEPRECATION")
        val display = wm.defaultDisplay

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val p = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(p) // full screen (bao gồm system bars)
            p.x to p.y
        } else {
            val p = Point()
            @Suppress("DEPRECATION")
            display.getSize(p) // fallback cũ hơn
            p.x to p.y
        }
    }

    return TargetSize(w, h)
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

fun Context.createDialog() = Dialog(this, R.style.FullScreenDialog).apply {
    requestWindowFeature(FEATURE_NO_TITLE)
    create()
}

fun Dialog.showFullScreen() {
    val window = this.window ?: return
    val activity = context as? Activity
    if (activity?.isFinishing == true || activity?.isDestroyed == true) return
    show()
    window.setLayout(MATCH_PARENT, MATCH_PARENT)
    window.setGravity(CENTER)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.hide(WindowInsetsCompat.Type.systemBars())
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}

fun Context.canHandleIntent(intent: Intent): Boolean {
    val pm = packageManager
    val list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return list.isNotEmpty()
}



