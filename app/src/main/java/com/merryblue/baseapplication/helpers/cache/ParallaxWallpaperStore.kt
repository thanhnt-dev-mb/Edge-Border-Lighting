package com.merryblue.baseapplication.helpers.cache

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.helpers.ServiceState
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ParallaxWallpaperStore {
    private const val LEGACY_FILE_NAME = "parallax_wallpaper_bg.png"
    private const val FILE_PREFIX = "parallax_wallpaper_bg_"
    private const val PENDING_FILE_PREFIX = "parallax_wallpaper_pending_"
    private const val FILE_EXTENSION = ".png"

    fun saveAndNotify(context: Context, bitmap: Bitmap, quality: Int = 92) {
        val appContext = context.applicationContext
        saveFile(appContext, bitmap, quality)
        notifyChanged(appContext)
    }

    fun saveFile(context: Context, bitmap: Bitmap, quality: Int = 92) {
        val appContext = context.applicationContext
        val file = writeBitmapToNewFile(appContext, FILE_PREFIX, bitmap, quality)
        AppPreferences(appContext).parallaxWallpaperPath = file.absolutePath
        deleteOldCacheFiles(appContext, keepPath = file.absolutePath)
    }

    fun savePendingFile(context: Context, bitmap: Bitmap, quality: Int = 92): String {
        val appContext = context.applicationContext
        cleanupPendingFiles(appContext)
        return writeBitmapToNewFile(appContext, PENDING_FILE_PREFIX, bitmap, quality).absolutePath
    }

    fun commitPendingFile(context: Context, pendingPath: String): Boolean {
        val appContext = context.applicationContext
        val pendingFile = File(pendingPath)
        if (!pendingFile.isInternalPendingFile(appContext)) return false
        if (!pendingFile.exists()) return false

        val file = createCacheFile(appContext, FILE_PREFIX)
        val committed = runCatching {
            if (!pendingFile.renameTo(file)) {
                pendingFile.copyTo(file, overwrite = true)
                pendingFile.delete()
            }
            file.exists() && file.length() > 0L
        }.getOrDefault(false)

        if (!committed) {
            file.delete()
            return false
        }

        AppPreferences(appContext).parallaxWallpaperPath = file.absolutePath
        deleteOldCacheFiles(appContext, keepPath = file.absolutePath)
        cleanupPendingFiles(appContext)
        return true
    }

    fun deletePendingFile(context: Context, pendingPath: String?) {
        val appContext = context.applicationContext
        val pendingFile = pendingPath?.let(::File) ?: return
        if (pendingFile.isInternalPendingFile(appContext)) {
            runCatching { pendingFile.delete() }
        }
    }

    fun notifyChanged(context: Context) {
        val appContext = context.applicationContext
        appContext.sendBroadcast(
            Intent(ServiceState.ACTION_PARALLAX_WALLPAPER_CHANGED)
                .setPackage(appContext.packageName)
        )
    }

    private fun writeBitmapToNewFile(context: Context, prefix: String, bitmap: Bitmap, quality: Int): File {
        val file = createCacheFile(context, prefix)
        val tempFile = File(context.filesDir, "${file.name}.tmp")

        val saved = FileOutputStream(tempFile).use { out ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, quality, out)
            out.flush()
            compressed
        }
        if (!saved) {
            tempFile.delete()
            error("Failed to compress parallax wallpaper bitmap")
        }

        if (!tempFile.renameTo(file)) {
            tempFile.copyTo(file, overwrite = true)
            tempFile.delete()
        }
        return file
    }

    private fun createCacheFile(context: Context, prefix: String): File {
        return File(
            context.filesDir,
            "$prefix${System.currentTimeMillis()}_${UUID.randomUUID()}$FILE_EXTENSION"
        )
    }

    private fun deleteOldCacheFiles(context: Context, keepPath: String) {
        context.filesDir.listFiles { file ->
            file.name == LEGACY_FILE_NAME ||
                (file.name.startsWith(FILE_PREFIX) && file.name.endsWith(FILE_EXTENSION))
        }?.forEach { file ->
            if (file.absolutePath != keepPath) {
                runCatching { file.delete() }
            }
        }
    }

    private fun cleanupPendingFiles(context: Context) {
        context.filesDir.listFiles { file ->
            file.name.startsWith(PENDING_FILE_PREFIX) && file.name.endsWith(FILE_EXTENSION)
        }?.forEach { file ->
            runCatching { file.delete() }
        }
    }

    private fun File.isInternalPendingFile(context: Context): Boolean {
        val filesDir = context.filesDir.canonicalFile
        val file = canonicalFile
        return file.parentFile == filesDir &&
            file.name.startsWith(PENDING_FILE_PREFIX) &&
            file.name.endsWith(FILE_EXTENSION)
    }
}
