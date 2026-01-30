package com.merryblue.baseapplication.ui.widget

import android.os.Build
import com.merryblue.baseapplication.databinding.BottomSheetWallpaperTargetBinding
import org.app.core.base.BaseBottomSheetFragment

enum class WallpaperTarget { HOME, LOCK, BOTH }

class BottomSheetWallpaperTarget(
    var onSelected: (WallpaperTarget) -> Unit
): BaseBottomSheetFragment<BottomSheetWallpaperTargetBinding>() {

    override fun initDialog() {
        binding.apply {
            val supportsLock = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

            btnLockScreen.isEnabled = supportsLock
            btnBothScreen.isEnabled = supportsLock
            btnLockScreen.alpha = if (supportsLock) 1f else 0.4f
            btnBothScreen.alpha = if (supportsLock) 1f else 0.4f

            btnHomeScreen.setOnClickListener {
                onSelected.invoke(WallpaperTarget.HOME)
                dismissAllowingStateLoss()
            }

            btnLockScreen.setOnClickListener {
                if (!supportsLock) return@setOnClickListener
                onSelected.invoke(WallpaperTarget.LOCK)
                dismissAllowingStateLoss()
            }

            btnBothScreen.setOnClickListener {
                if (!supportsLock) return@setOnClickListener
                onSelected.invoke(WallpaperTarget.BOTH)
                dismissAllowingStateLoss()
            }
        }
    }

    companion object {
        const val TAG = "bottom_sheet_wallpaper_target"
    }
}