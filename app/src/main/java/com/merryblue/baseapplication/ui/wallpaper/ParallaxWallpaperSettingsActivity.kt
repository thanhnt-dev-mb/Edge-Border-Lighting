package com.merryblue.baseapplication.ui.wallpaper

import android.app.ActivityManager
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityParallaxWallpaperSettingsBinding
import com.merryblue.baseapplication.domain.repository.EdgeImageRepository
import com.merryblue.baseapplication.domain.repository.EdgeImageSource
import com.merryblue.baseapplication.domain.repository.TargetSize
import com.merryblue.baseapplication.enums.InterstitialFunction
import com.merryblue.baseapplication.helpers.cache.ParallaxWallpaperStore
import com.merryblue.baseapplication.helpers.getFullScreenTargetSize
import com.merryblue.baseapplication.helpers.openProperNetworkSettings
import com.merryblue.baseapplication.service.edge.EdgeLightingOverlayService
import com.merryblue.baseapplication.service.edge.ParallaxWallpaperService
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.widget.BottomSheetEdgePermission
import com.merryblue.baseapplication.ui.widget.BottomSheetNoInternet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.app.core.base.BaseActivity
import org.app.core.base.binding.setOnSingleClickListener
import org.app.core.base.extensions.toastMsg
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

@AndroidEntryPoint
class ParallaxWallpaperSettingsActivity : BaseActivity<ActivityParallaxWallpaperSettingsBinding>() {

    @Inject
    lateinit var edgeImageRepository: EdgeImageRepository

    private val prefs by lazy { AppPreferences(this) }
    private val edgePermissionViewModel: EdgePermissionViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    override val nativeHeight: Int
        get() = -1

    private var pendingBackgroundLoadJob: Job? = null
    private var storedPreviewLoadJob: Job? = null
    private var previewBitmap: Bitmap? = null
    private var isPendingBackgroundLoading = false
    private var pendingWallpaperPath: String? = null

    private val setLiveWallpaperLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (isMyLiveWallpaperActive()) {
            checkPermissionOverlay()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(this)) {
            prefs.edgeState = prefs.edgeState.copy(isEnableEdgeLighting = true)
            startEdgeOverlay()
        } else {
            prefs.edgeState = prefs.edgeState.copy(isEnableEdgeLighting = false)
            finish()
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_parallax_wallpaper_settings

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
    }

    override fun setUpViews() {
        isPendingBackgroundLoading = hasPendingBackgroundRequest()
        if (isPendingBackgroundLoading) clearPreview() else showCurrentPreview()
        updatePendingBackgroundUi(isPendingBackgroundLoading)
        loadPendingBackgroundIfNeeded()
        registerClicks()
    }

    override fun setUpObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    edgePermissionViewModel.edgePermission.collect {
                        val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${packageName}".toUri())
                        overlayPermissionLauncher.launch(i)
                    }
                }

                launch {
                    homeViewModel.connectionState.collectLatest {
                        onNetworkStateChanged(it)
                        handleNoInternetBottomSheet(it)
                    }
                }
            }
        }
    }

    private fun registerClicks() {
        binding.btnSetWallpaper.setOnSingleClickListener {
            showInterstitialBy(InterstitialFunction.SetParallaxWallpaper.name) {
                homeViewModel.increaseUsageCount()
                onClickSetParallaxWallpaper()
            }
        }
        binding.btnBackWallpaper.setOnSingleClickListener { finish() }
    }

    private fun handleNoInternetBottomSheet(isConnected: Boolean) {
        val fm = supportFragmentManager
        val current = fm.findFragmentByTag(BottomSheetNoInternet.TAG) as? BottomSheetDialogFragment

        if (isConnected) {
            if (current?.dialog?.isShowing == true) current.dismissAllowingStateLoss()
            return
        }

        if (current?.dialog?.isShowing == true) return

        BottomSheetNoInternet.newInstance {
            this.openProperNetworkSettings()
        }.show(fm, BottomSheetNoInternet.TAG)
    }

    private fun showCurrentPreview() {
        val path = prefs.parallaxWallpaperPath?.takeIf { it.isNotBlank() } ?: run {
            clearPreview()
            return
        }
        if (previewBitmap != null) {
            binding.parallaxPreview.setPreviewBitmap(previewBitmap)
            return
        }
        storedPreviewLoadJob?.cancel()
        storedPreviewLoadJob = lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodePreviewBitmap(path) }
            if (isFinishing || isDestroyed) {
                bitmap?.takeIf { !it.isRecycled }?.recycle()
                return@launch
            }
            updatePreviewBitmap(bitmap)
        }
    }

    private fun clearPreview() {
        updatePreviewBitmap(null)
    }

    private fun loadPendingBackgroundIfNeeded() {
        val pendingSource = buildPendingBackgroundSource() ?: return
        pendingBackgroundLoadJob?.cancel()
        pendingBackgroundLoadJob = lifecycleScope.launch {
            updatePendingBackgroundUi(true)
            val previewTarget = buildPreviewTargetSize()
            val pendingPreview = withContext(Dispatchers.IO) {
                val wallpaperBitmap = edgeImageRepository.loadBitmap(pendingSource, getFullScreenTargetSize())
                    ?.takeIf { !it.isRecycled }
                    ?: return@withContext null
                val previewBitmap = createPreviewBitmap(wallpaperBitmap, previewTarget)
                if (previewBitmap == null) {
                    wallpaperBitmap.takeIf { !it.isRecycled }?.recycle()
                    return@withContext null
                }

                val pendingPath = runCatching {
                    ParallaxWallpaperStore.savePendingFile(this@ParallaxWallpaperSettingsActivity, wallpaperBitmap)
                }.getOrNull()

                if (previewBitmap !== wallpaperBitmap && !wallpaperBitmap.isRecycled) {
                    wallpaperBitmap.recycle()
                }

                if (pendingPath == null) {
                    previewBitmap.takeIf { !it.isRecycled }?.recycle()
                    return@withContext null
                }
                PendingPreview(previewBitmap, pendingPath)
            }

            if (pendingPreview == null) {
                updatePendingBackgroundUi(false)
                toastMsg(getString(R.string.wallpaper_set_failed))
                finish()
                return@launch
            }

            clearPendingWallpaper()
            pendingWallpaperPath = pendingPreview.pendingPath
            updatePreviewBitmap(pendingPreview.bitmap)
            updatePendingBackgroundUi(false)
        }
    }

    private fun updatePendingBackgroundUi(loading: Boolean) {
        isPendingBackgroundLoading = loading
        binding.loadingContainer.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSetWallpaper.isEnabled = !loading
        binding.btnSetWallpaper.alpha = if (loading) 0.5f else 1f
    }

    private fun onClickSetParallaxWallpaper() {
        if (isPendingBackgroundLoading) return
        if (!commitPendingWallpaperIfNeeded()) {
            toastMsg(getString(R.string.wallpaper_set_failed))
            return
        }

        if (prefs.parallaxWallpaperPath.isNullOrBlank()) {
            toastMsg(getString(R.string.wallpaper_set_failed))
            return
        }

        if (isMyLiveWallpaperActive()) {
            ParallaxWallpaperStore.notifyChanged(this)
            checkPermissionOverlay()
            return
        }

        openSystemLiveWallpaperPicker(ComponentName(this, ParallaxWallpaperService::class.java))
    }

    private fun commitPendingWallpaperIfNeeded(): Boolean {
        val pendingPath = pendingWallpaperPath ?: return true
        val committed = ParallaxWallpaperStore.commitPendingFile(this, pendingPath)
        if (committed) {
            pendingWallpaperPath = null
        }
        return committed
    }

    private fun clearPendingWallpaper() {
        ParallaxWallpaperStore.deletePendingFile(this, pendingWallpaperPath)
        pendingWallpaperPath = null
    }

    private fun openSystemLiveWallpaperPicker(service: ComponentName) {
        val changeIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, service)
        }

        try {
            when {
                prefs.canChangeLive -> setLiveWallpaperLauncher.launch(changeIntent)
                prefs.canLiveChooser -> setLiveWallpaperLauncher.launch(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                else -> toastMsg(getString(R.string.this_device_does_not_support_installing_live_wallpaper))
            }
        } catch (_: ActivityNotFoundException) {
            toastMsg(getString(R.string.this_device_does_not_support_installing_live_wallpaper))
        }
    }

    private fun checkPermissionOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            toastMsg(getString(R.string.live_wallpaper_set_success))
            showBottomSheetEdgePermission()
            return
        }

        toastMsg(getString(R.string.live_wallpaper_set_success))
        startEdgeOverlay()
    }

    private fun startEdgeOverlay() {
        ContextCompat.startForegroundService(this, Intent(this, EdgeLightingOverlayService::class.java))
        finish()
    }

    private fun showBottomSheetEdgePermission() {
        (supportFragmentManager.findFragmentByTag(BottomSheetEdgePermission.TAG) as? BottomSheetDialogFragment)
            ?.dismissAllowingStateLoss()
        BottomSheetEdgePermission.newInstance().show(supportFragmentManager, BottomSheetEdgePermission.TAG)
    }

    private fun isMyLiveWallpaperActive(): Boolean {
        val activityManager = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return activityManager.getRunningServices(Int.MAX_VALUE).any { it.service.className == ParallaxWallpaperService::class.java.name }
    }

    private fun hasPendingBackgroundRequest(): Boolean {
        return !intent.getStringExtra(EXTRA_PENDING_BACKGROUND_URL).isNullOrBlank() ||
            !intent.getStringExtra(EXTRA_PENDING_BACKGROUND_URI_STRING).isNullOrBlank()
    }

    private fun buildPendingBackgroundSource(): EdgeImageSource? {
        val pendingUrl = intent.getStringExtra(EXTRA_PENDING_BACKGROUND_URL)?.takeIf { it.isNotBlank() }
        if (pendingUrl != null) return EdgeImageSource.Url(pendingUrl)

        val pendingUri = intent.getStringExtra(EXTRA_PENDING_BACKGROUND_URI_STRING)
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
        return pendingUri?.let(EdgeImageSource::UriSource)
    }

    private fun updatePreviewBitmap(bitmap: Bitmap?) {
        if (previewBitmap === bitmap) return
        recyclePreviewBitmap(except = bitmap)
        previewBitmap = bitmap
        if (bitmap == null) {
            binding.parallaxPreview.clearPreview()
        } else {
            binding.parallaxPreview.setPreviewBitmap(bitmap)
        }
    }

    private fun recyclePreviewBitmap(except: Bitmap? = null) {
        previewBitmap?.takeIf { it !== except && !it.isRecycled }?.recycle()
        if (previewBitmap !== except) {
            previewBitmap = null
        }
    }

    private fun buildPreviewTargetSize(): TargetSize {
        val displayMetrics = resources.displayMetrics
        var width = (displayMetrics.widthPixels * PREVIEW_BITMAP_SCALE_FACTOR).roundToInt().coerceAtLeast(1)
        var height = (displayMetrics.heightPixels * PREVIEW_BITMAP_SCALE_FACTOR).roundToInt().coerceAtLeast(1)
        val area = width.toLong() * height.toLong()
        if (area > PREVIEW_MAX_AREA_PX) {
            val scale = sqrt(PREVIEW_MAX_AREA_PX.toDouble() / area.toDouble()).toFloat()
            width = (width * scale).roundToInt().coerceAtLeast(1)
            height = (height * scale).roundToInt().coerceAtLeast(1)
        }
        return TargetSize(width = width, height = height)
    }

    private fun createPreviewBitmap(source: Bitmap, target: TargetSize): Bitmap? {
        if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
        val widthScale = target.width / source.width.toFloat()
        val heightScale = target.height / source.height.toFloat()
        val scale = min(1f, min(widthScale, heightScale))
        if (scale >= 0.999f) {
            return source
        }

        val previewWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
        val previewHeight = (source.height * scale).roundToInt().coerceAtLeast(1)
        return runCatching {
            Bitmap.createScaledBitmap(source, previewWidth, previewHeight, true).also { it.prepareToDraw() }
        }.getOrNull()
    }

    private fun decodePreviewBitmap(path: String): Bitmap? {
        val target = buildPreviewTargetSize()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, target.width, target.height)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeFile(path, options)?.also { it.prepareToDraw() }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
                halfHeight /= 2
                halfWidth /= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    override fun onDestroy() {
        pendingBackgroundLoadJob?.cancel()
        storedPreviewLoadJob?.cancel()
        clearPendingWallpaper()
        binding.parallaxPreview.release()
        recyclePreviewBitmap()
        super.onDestroy()
    }

    private data class PendingPreview(
        val bitmap: Bitmap,
        val pendingPath: String
    )

    companion object {
        const val EXTRA_PENDING_BACKGROUND_URL = "extra_pending_background_url"
        const val EXTRA_PENDING_BACKGROUND_URI_STRING = "extra_pending_background_uri_string"
        private const val PREVIEW_BITMAP_SCALE_FACTOR = 0.82f
        private const val PREVIEW_MAX_AREA_PX = 1_500_000L
    }
}
