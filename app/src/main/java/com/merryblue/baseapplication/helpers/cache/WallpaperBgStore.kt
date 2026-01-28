package com.merryblue.baseapplication.helpers.cache

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.helpers.ServiceState
import java.io.File
import java.io.FileOutputStream

object WallpaperBgStore {
    private const val FILE_NAME = "edge_lighting_bg.png"

    fun saveRippleAndNotify(context: Context, bitmap: Bitmap, quality: Int = 92) {
        val appCtx = context.applicationContext
        saveFile(appCtx, bitmap, quality)
        appCtx.sendBroadcast(Intent(ServiceState.ACTION_RIPPLE_BG_CHANGED))
    }

    fun saveFile(context: Context, bitmap: Bitmap, quality: Int = 92) {
        val file = File(context.filesDir, FILE_NAME)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, quality.coerceIn(50, 100), out)
            out.flush()
        }
        AppPreferences(context).backgroundPath = file.absolutePath
    }
}