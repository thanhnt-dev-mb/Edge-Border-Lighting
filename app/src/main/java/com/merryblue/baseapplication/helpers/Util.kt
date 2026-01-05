package com.merryblue.baseapplication.helpers

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.webkit.CookieManager
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.google.android.material.snackbar.Snackbar
import com.merryblue.baseapplication.R
import org.app.core.base.utils.StringResId
import java.io.File
import java.io.FileOutputStream
import java.util.Locale


// Constants
const val ACTION_FILTER_BIOMETRIC_AUTH_RESULT = "com.app.authentication_result.filter"
const val EXTRA_AUTH_RESULT = "key_extra_authentication_result"
const val INTENT_SONG_SELECTED = "intent_song_selected"
const val TIME_FOR_TASK = 1000/24.toLong()

fun File.copyToDir(parentDir: File, prefixName: String): File {
    val newFile = File(parentDir.absolutePath, prefixName + this.name)
    return FileOutputStream(newFile).use { fos ->
        fos.write(this.inputStream().readBytes())
        fos.flush()
        newFile
    }
}

fun File.copy(dest: File) {
    return FileOutputStream(dest).use { fos ->
        fos.write(this.inputStream().readBytes())
        fos.flush()
    }
}

fun Bitmap.CompressFormat.toFileExtension() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    when (this) {
        Bitmap.CompressFormat.PNG -> ".png"
        Bitmap.CompressFormat.JPEG -> ".jpg"
        Bitmap.CompressFormat.WEBP_LOSSLESS -> ".webp"
        Bitmap.CompressFormat.WEBP_LOSSY -> ".webp"
        Bitmap.CompressFormat.WEBP -> ".webp"
        else -> throw IllegalArgumentException("Invalid compression format: $this")
    }
} else {
    when (this) {
        Bitmap.CompressFormat.PNG -> ".png"
        Bitmap.CompressFormat.JPEG -> ".jpg"
        Bitmap.CompressFormat.WEBP -> ".webp"
        else -> throw IllegalArgumentException("Invalid compression format: $this")
    }
}

fun File.getExtensionAsBitmapFormat(): Bitmap.CompressFormat {
    return when (this.extension) {
        "png" -> Bitmap.CompressFormat.JPEG
        "jpeg", "jpg" -> Bitmap.CompressFormat.JPEG
        "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        else -> throw IllegalArgumentException("Invalid extension to Bitmap: ${this.extension}")
    }
}

fun File.getExtensionAsQuality(): Int {
    return when (this.extension) {
        "png", "jpeg", "jpg" -> 50
        "webp" -> 25
        else -> throw IllegalArgumentException("Invalid extension to Bitmap: ${this.extension}")
    }
}

fun saveFolder(folderPath: (String) -> Unit) {
    val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
    val folder = "$directoryPath/CompressImage"
    if (!File(folder).exists()) {
        File(folder).mkdir()
    }

    folderPath.invoke(File(folder).path)
}

fun Context.termCompressLargeImage(path: String): String? =
    run {
        cacheDir?.let { cacheDir ->
            try {
                val bitmap = BitmapFactory.decodeFile(path).resizeBitmap(1600)
                val temp = File("${cacheDir.path}/rmbg_term_compress.jpg")
                if (temp.exists()) temp.delete()
                val out = FileOutputStream(temp)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
                bitmap.recycle()
                out.flush()
                out.close()
                return temp.path
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

fun Context.termRmBgImage(bitmap: Bitmap): String? =
    run {
        cacheDir?.let { cacheDir ->
            try {
                val temp = File("${cacheDir.path}/rmbg_term_image.png")
                if (temp.exists()) temp.delete()
                val out = FileOutputStream(temp)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()
                return temp.path
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

fun Context.termWebpToJpg(path: String): String? =
    run {
        cacheDir?.let { cacheDir ->
            try {
                val webpBitmap = BitmapFactory.decodeFile(path)

                val temp = File("${cacheDir.path}/webp_term_image.png")
                if (temp.exists()) temp.delete()
                val out = FileOutputStream(temp)
                webpBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()
                return temp.path
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

fun File.getAsBitmap(
    options: BitmapFactory.Options? = null
): Bitmap {
    val bitmap = BitmapFactory.decodeFile(this.absolutePath, options)
//    val exif = ExifInterface(this.absolutePath)
//    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
//    val matrix = Matrix().apply {
//        when (orientation) {
//            6 -> postRotate(90f)
//            3 -> postRotate(180f)
//            8 -> postRotate(270f)
//        }
//    }
//    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    return bitmap
}

fun Bitmap.resizeBitmap(max: Int = 3000): Bitmap {
    val bitmap: Bitmap = if (this.width < max) {
        this
    } else {
        val srcWidth = this.width
        val srcHeight = this.height
        val ratio = (max.toDouble() / this.width)
        val dstWidth = (srcWidth * ratio).toInt()
        val dstHeight = (srcHeight * ratio).toInt()
        Bitmap.createScaledBitmap(this, dstWidth, dstHeight, true)
    }
    return bitmap
}

fun File.convertPathToNewFormat(format: Bitmap.CompressFormat) =
    File(absolutePath.substringBeforeLast(".") + format.toFileExtension())

fun File.convertFileNameToNewFormat(format: Bitmap.CompressFormat) =
    name.substringBeforeLast(".") + format.toFileExtension()

fun Context.getPdfThumbnail(uri: Uri) : Bitmap? {
    contentResolver.openFileDescriptor(uri, "r")?.use { parcelFileDescriptor ->
        val pdfRenderer = PdfRenderer(parcelFileDescriptor).openPage(0)
        val bitmap = Bitmap.createBitmap(pdfRenderer.width, pdfRenderer.height, Bitmap.Config.ARGB_8888)
        pdfRenderer.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        pdfRenderer.close()
        
        return bitmap
    }
    
    return null
}

fun Context.shareFile(path: String, mimeType: String = "application/pdf") {
    val externalUri = FileProvider.getUriForFile(this, this.packageName + ".fileprovider", File(path))
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, externalUri)
        type = mimeType
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(shareIntent, getString(StringResId.share)))
}

fun Context.shareMultiple(filePaths: List<String>) {
    val uris = ArrayList<Uri>()
    for (path in filePaths) {
        val uri = FileProvider.getUriForFile(this, this.packageName + ".fileprovider", File(path))
        uris.add(uri)
    }
    val intent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        type = "*/*"
    }
    try {
        startActivity(Intent.createChooser(intent, getString(StringResId.share)))
    } catch (_: Exception) {}
}

fun Uri.toPdfThumbnail(context: Context) : Bitmap? {
    with(context) {
        try {
            contentResolver.openFileDescriptor(this@toPdfThumbnail, "r")?.use { parcelFileDescriptor ->
                val pdfRenderer = PdfRenderer(parcelFileDescriptor).openPage(0)
                val bitmap = Bitmap.createBitmap(pdfRenderer.width, pdfRenderer.height, Bitmap.Config.ARGB_8888)
                pdfRenderer.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pdfRenderer.close()
                
                return bitmap
            }
        } catch (_: Exception) { return null }
        
        return null
    }
}

fun Uri.getName(context: Context) : String {
    var cursor: Cursor? = null
    try {
        cursor = context.contentResolver.query(this, arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA
        ), null, null, null)
        
        if (cursor != null) {
            cursor.moveToNext()
            
            return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
        }
    } finally {
        cursor?.close()
    }
    
    return ""
}

fun Toolbar.setNavigationIconColor(@ColorInt color: Int) = navigationIcon?.mutate()?.let {
    it.setTint(color)
    this.navigationIcon = it
}

fun getCurrentLanguage() : String {
    val locates = Resources.getSystem().configuration.locales
    return if (locates.isEmpty) {
        Locale.getDefault().language
    } else {
        locates.get(0).language
    }
}

fun checkIsLoggedFBInByCookie(): Boolean {
    val cookieManager = CookieManager.getInstance()
    val mCookies = cookieManager.getCookie("https://m.facebook.com/")
    if (mCookies != null && mCookies.contains("c_user=")) {
        return true
    }

    val cookies = cookieManager.getCookie("https://facebook.com/")
    if (cookies != null && cookies.contains("c_user=")) {
        return true
    }

    return false
}

fun Context.isAppInstalled(packageName: String): Boolean {
    return try {
        packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }
}

fun TextView.append(string: String?, @ColorRes color: Int) {
    if (string.isNullOrEmpty()) {
        return
    }

    val spannable: Spannable = SpannableString(string)
    spannable.setSpan(
        ForegroundColorSpan(ContextCompat.getColor(context, color)),
        0,
        spannable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    append(spannable)
}

fun isBackground(): Boolean {
    val runningAppProcessInfo = ActivityManager.RunningAppProcessInfo()
    ActivityManager.getMyMemoryState(runningAppProcessInfo)

    return runningAppProcessInfo.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
}

fun TextView.append(string: String, @ColorRes color: Int, typeface: Typeface? = null) {
    if (string.isEmpty()) {
        return
    }

    val spannable: Spannable = SpannableString(string)
    spannable.setSpan(
        ForegroundColorSpan(ContextCompat.getColor(context, color)),
        0,
        spannable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    typeface?.let {
        spannable.setSpan(
            StyleSpan(it.style),
            0,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    append(spannable)
}

fun String.append(txt: String, @ColorInt color: Int, typeface: Typeface? = null) : SpannableStringBuilder {
    val spannableStringBuilder = SpannableStringBuilder(this)
    val spannable: Spannable = SpannableString(txt)
    spannable.setSpan(
        ForegroundColorSpan(color),
        0,
        spannable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    typeface?.let {
        spannable.setSpan(
            StyleSpan(it.style),
            0,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    spannableStringBuilder.append(spannable)

    return spannableStringBuilder
}

fun ConstraintLayout.setHorizontalBias(
    @IdRes targetViewId: Int,
    bias: Float
) {
    val constraintSet = ConstraintSet()
    constraintSet.clone(this)
    constraintSet.setHorizontalBias(targetViewId, bias)
    constraintSet.applyTo(this)
}

fun TextView.setDrawableTint(color: Int) {
    setTextColor(color)
    for (drawable in getCompoundDrawablesRelative()) {
        if (drawable != null) {
            drawable.colorFilter =
                PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }
}

fun Context.openPolicy() {
    try {
        val intent = Intent(Intent.ACTION_VIEW, "https://dj-mixer.merryblue.llc/privacy.html".toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}