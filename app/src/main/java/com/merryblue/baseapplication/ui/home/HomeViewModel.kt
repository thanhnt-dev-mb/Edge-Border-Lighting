package com.merryblue.baseapplication.ui.home

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.Topic
import com.merryblue.baseapplication.domain.repository.EdgeDataRepository
import com.merryblue.baseapplication.domain.repository.EdgeImageRepository
import com.merryblue.baseapplication.domain.repository.EdgeImageSource
import com.merryblue.baseapplication.domain.repository.TargetSize
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import com.merryblue.baseapplication.helpers.BackgroundType
import com.merryblue.baseapplication.helpers.EdgeStyle.EDGE_LINEAR
import com.merryblue.baseapplication.helpers.EdgeStyle.EDGE_NONE
import com.merryblue.baseapplication.helpers.PreviewType.EDGE_WALLPAPER_SCREEN
import com.merryblue.baseapplication.helpers.PreviewType.RIPPLE_WALLPAPER_SCREEN
import com.merryblue.baseapplication.helpers.WallpaperType
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.service.EdgeLightingOverlayService
import com.merryblue.baseapplication.ui.iap.BillingRepository
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeLightingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.app.core.base.BaseViewModel
import timber.log.Timber
import javax.inject.Inject
import kotlin.Float
import kotlin.Int

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val appRepository: AppRepository,
    private val billingRepository: BillingRepository,
    private val edgeDataRepository: EdgeDataRepository,
    private val edgeImageRepository: EdgeImageRepository
) : BaseViewModel(application) {

    private val _presetState = MutableStateFlow<Topic?>(null)
    val presetState: StateFlow<Topic?> = _presetState.asStateFlow()

    private val _themeState = MutableStateFlow<Topic?>(null)
    val themeState: StateFlow<Topic?> = _themeState.asStateFlow()

    private val _settingsEdgeLighting = MutableSharedFlow<Unit>(replay = 0)
    val settingsEdgeLighting = _settingsEdgeLighting.asSharedFlow()

    private val _bgBitmap = MutableSharedFlow<Pair<String, Bitmap?>>(replay = 0)
    val bgBitmap = _bgBitmap.asSharedFlow()

    val connectionState = appRepository.networkState

    var isStartSession
        get() = appRepository.isStartSession
        set(value) {
            appRepository.isStartSession = value
        }

    val serviceRunning: Boolean
        get() = appRepository.isServiceRunning

    val lockedAppCount: Int
        get() = appRepository.lockedAppCount

    fun isPremium() = billingRepository.isPurchased()

    fun getRemoteConfiguration() = appRepository.loadAdsConfiguration()

    fun isRated() = appRepository.rated >= 4

    fun setRate(rate: Int) {
        appRepository.rated = rate
    }

    fun applySettingEdgeLighting() {
        if (EdgeLightingOverlayService.isRunning) return
        viewModelScope.launch {
            _settingsEdgeLighting.emit(Unit)
        }
    }

    fun loadPreset(topicKey: String) {
        _presetState.value = edgeDataRepository.getDataTopic(topicKey)
    }

    fun loadThemes(topicKey: String) {
        _themeState.value = edgeDataRepository.getDataTopic(topicKey)
    }

    var videoUrl: String
        get() = appRepository.videoUrl
        set(value) { appRepository.videoUrl = value }

    var rippleEffectUrl: String
        get() = appRepository.rippleEffectUrl
        set(value) { appRepository.rippleEffectUrl = value }

    var edgeState: EdgeLightingState
        get() = appRepository.edgeState
        set(value) { appRepository.edgeState = value }

    private fun getPresetEdgeLighting(item: Item): EdgeLightingState {
        val colorsInt = item.colors?.map(Color::parseColor)?.toIntArray()
        return edgeState.copy(
            notchType = Advanced.NOTCH_DEFAULT,
            direction = Advanced.DIRECTION_CLOCKWISE,
            edgeStyleType = if (colorsInt != null) EDGE_LINEAR else EDGE_NONE,
            backgroundType = BackgroundType.BACKGROUND_URL,
            backgroundImageUrl = item.pathUrl,
            backgroundImageUriString = null,
            backgroundImageResId = 0,
            colors = colorsInt ?: edgeState.colors,
            vectorResId = R.drawable.ic_none,
            iconSizePx = 8f.dpToPx,
            advancePx = 18f.dpToPx,
            rotate = true,
            phaseMultiplier = 0.1f,
            speedMs = 2500L,
            topRadius = 24f.dpToPx,
            bottomRadius = 24f.dpToPx,
        )
    }

    fun updateEdgeState(block: (EdgeLightingState) -> EdgeLightingState) {
        edgeState = block.invoke(edgeState)
        sendActionBroadcast(ACTION_EDGE_OVERLAY_CHANGED)
    }

    fun loadBackgroundRippleUrl(item: Item, target: TargetSize) {
        viewModelScope.launch {
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.Url(item.pathUrl), target)
            _bgBitmap.emit(Pair(RIPPLE_WALLPAPER_SCREEN, bmp))
        }
    }

    fun loadEdgeBackgroundUrl(item: Item, target: TargetSize) {
        Timber.tag("Log_Colors").d("color: ${item.colors}")
        edgeState = getPresetEdgeLighting(item)

        viewModelScope.launch {
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.Url(item.pathUrl), target)
            bmp?.let { sendActionBroadcast(ACTION_EDGE_OVERLAY_CHANGED) }
            _bgBitmap.emit(Pair(EDGE_WALLPAPER_SCREEN, bmp))
        }
    }

    fun loadStaticBackgroundUrl(item: Item, target: TargetSize) {
        if (item.type == WallpaperType.TYPE_EDGE) {
            Timber.tag("Log_Colors").d("color: ${item.colors}")
            edgeState = getPresetEdgeLighting(item)
        }

        viewModelScope.launch {
            val originalUrl = if (item.type == WallpaperType.TYPE_VIDEO) item.thumbUrl else item.pathUrl
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.Url(originalUrl), target)
            bmp?.let { sendActionBroadcast(ACTION_EDGE_OVERLAY_CHANGED) }
            _bgBitmap.emit(Pair(EDGE_WALLPAPER_SCREEN, bmp))
        }
    }

    fun loadBackgroundUri(uri: Uri, target: TargetSize) {
        val newState = edgeState.copy(
            backgroundType = BackgroundType.BACKGROUND_URI,
            backgroundImageUriString = uri.toString(),
            backgroundImageUrl = null,
            backgroundImageResId = 0
        )
        edgeState = newState

        viewModelScope.launch {
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.UriSource(uri), target)
            _bgBitmap.emit(Pair(EDGE_WALLPAPER_SCREEN, bmp))
        }
    }

    fun onClickBackgroundRes(resId: Int, target: TargetSize) {
        val newState = edgeState.copy(
            backgroundType = BackgroundType.BACKGROUND_RES,
            backgroundImageResId = resId,
            backgroundImageUrl = null,
            backgroundImageUriString = null
        )
        edgeState = newState

        viewModelScope.launch {
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.Res(resId), target)
            _bgBitmap.emit(Pair(EDGE_WALLPAPER_SCREEN, bmp))
        }
    }

    fun sendActionBroadcast(action: String) {
        val ctx = application.applicationContext
        val i = Intent(action).apply {
            setPackage(ctx.packageName)
        }
        ctx.sendBroadcast(i)
    }
}
