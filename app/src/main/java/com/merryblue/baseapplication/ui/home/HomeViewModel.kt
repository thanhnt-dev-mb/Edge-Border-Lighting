package com.merryblue.baseapplication.ui.home

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.coredata.model.edge.DisplayNotchType
import com.merryblue.baseapplication.coredata.model.edge.EdgeSelection
import com.merryblue.baseapplication.coredata.model.edge.EdgeSettings
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.Topic
import com.merryblue.baseapplication.domain.repository.EdgeDataRepository
import com.merryblue.baseapplication.domain.repository.EdgeImageRepository
import com.merryblue.baseapplication.domain.repository.EdgeImageSource
import com.merryblue.baseapplication.domain.repository.TargetSize
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import com.merryblue.baseapplication.helpers.BackgroundType
import com.merryblue.baseapplication.helpers.EdgeStyle.EDGE_LINEAR
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_STOP
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.ui.iap.BillingRepository
import com.merryblue.baseapplication.ui.view.edgelight.EdgeHoleShape
import com.merryblue.baseapplication.ui.view.edgelight.EdgeLightingState
import com.merryblue.baseapplication.ui.view.edgelight.InfinityShape
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

    private val _edgeColorEvents = MutableSharedFlow<EdgeSelection>(replay = 0, extraBufferCapacity = 1)
    val edgeColorEvents = _edgeColorEvents.asSharedFlow()

    private var _edgeSettingsEvents = MutableSharedFlow<EdgeSettings>(replay = 0, extraBufferCapacity = 1)
    val edgeSettingsEvents = _edgeSettingsEvents.asSharedFlow()

    private var _edgeAdvancedEvents = MutableSharedFlow<Advanced>(replay = 0, extraBufferCapacity = 1)
    val edgeAdvancedEvents = _edgeAdvancedEvents.asSharedFlow()

    private var _edgeDisplayNotchTypeEvents = MutableSharedFlow<DisplayNotchType>(replay = 0, extraBufferCapacity = 1)
    val edgeDisplayNotchTypeEvents = _edgeDisplayNotchTypeEvents.asSharedFlow()

    private var _edgeHoleTypeEvents = MutableSharedFlow<EdgeHoleShape>(replay = 0, extraBufferCapacity = 1)
    val edgeHoleTypeEvents = _edgeHoleTypeEvents.asSharedFlow()

    private var _edgeInfinityTypeEvents = MutableSharedFlow<InfinityShape>(replay = 0, extraBufferCapacity = 1)
    val edgeInfinityTypeEvents = _edgeInfinityTypeEvents.asSharedFlow()

    private var _edgeVisibilityEvents = MutableSharedFlow<Boolean>(replay = 0, extraBufferCapacity = 1)
    val edgeVisibilityEvents = _edgeVisibilityEvents.asSharedFlow()

    private val _bgBitmap = MutableSharedFlow<Bitmap?>(replay = 0)
    val bgBitmap = _bgBitmap.asSharedFlow()

    private val _restartOverlay = MutableSharedFlow<Boolean>(replay = 0)
    val restartOverlay = _restartOverlay.asSharedFlow()

    val connectionState = appRepository.networkState
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

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

    fun emitEdgeColor(event: EdgeSelection) {
        _edgeColorEvents.tryEmit(event)
    }

    fun emitEdgeSettings(event: EdgeSettings) {
        _edgeSettingsEvents.tryEmit(event)
    }

    fun emitEdgeAdvances(event: Advanced) {
        _edgeAdvancedEvents.tryEmit(event)
    }

    fun emitEdgeDisplayNotchType(event: DisplayNotchType) {
        _edgeDisplayNotchTypeEvents.tryEmit(event)
    }

    fun emitHoleType(event: EdgeHoleShape) {
        _edgeHoleTypeEvents.tryEmit(event)
    }

    fun emitInfinityType(event: InfinityShape) {
        _edgeInfinityTypeEvents.tryEmit(event)
    }

    fun emitVisibilityEdgeView(isShow: Boolean) {
        _edgeVisibilityEvents.tryEmit(isShow)
    }

    fun loadPreset(topicKey: String) {
        _presetState.value = edgeDataRepository.getDataTopic(topicKey)
    }

    fun loadThemes(topicKey: String) {
        _themeState.value = edgeDataRepository.getDataTopic(topicKey)
    }

    fun updateEdgeState(block: (EdgeLightingState) -> EdgeLightingState) {
        appRepository.edgeState = block.invoke(appRepository.edgeState)
        sendActionBroadcast(ACTION_EDGE_OVERLAY_CHANGED)
    }

    fun getEdgeState() = appRepository.edgeState

    fun onClickBackgroundUrl(item: Item, target: TargetSize) {

        appRepository.cacheEdgeState = appRepository.edgeState

        val colorsInt = item.colors?.map(Color::parseColor)?.toIntArray()
        val newState = appRepository.edgeState.copy(
            edgeStyleType = if (colorsInt != null) EDGE_LINEAR else appRepository.edgeState.edgeStyleType,
            backgroundType = BackgroundType.BACKGROUND_URL,
            backgroundImageUrl = item.pathUrl,
            backgroundImageUriString = null,
            backgroundImageResId = 0,
            colors = colorsInt ?: appRepository.edgeState.colors
        )

        appRepository.edgeState = newState

        viewModelScope.launch {
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.Url(item.pathUrl), target)
            bmp?.let { sendActionBroadcast(ACTION_EDGE_OVERLAY_STOP) }
            _bgBitmap.emit(bmp)
        }
    }

    fun onClickBackgroundUri(uri: Uri, target: TargetSize) {
        val newState = appRepository.edgeState.copy(
            backgroundType = BackgroundType.BACKGROUND_URI,
            backgroundImageUriString = uri.toString(),
            backgroundImageUrl = null,
            backgroundImageResId = 0
        )
        appRepository.edgeState = newState

        viewModelScope.launch {
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.UriSource(uri), target)
            bmp?.let { sendActionBroadcast(ACTION_EDGE_OVERLAY_STOP) }
            _bgBitmap.emit(bmp)
        }
    }

    fun onClickBackgroundRes(resId: Int, target: TargetSize) {
        val newState = appRepository.edgeState.copy(
            backgroundType = BackgroundType.BACKGROUND_RES,
            backgroundImageResId = resId,
            backgroundImageUrl = null,
            backgroundImageUriString = null
        )
        appRepository.edgeState = newState

        viewModelScope.launch {
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.Res(resId), target)
            bmp?.let { sendActionBroadcast(ACTION_EDGE_OVERLAY_STOP) }
            _bgBitmap.emit(bmp)
        }
    }

    fun restartOverlay() {
        viewModelScope.launch {
            if (appRepository.hasCacheEdgeState()) {
                appRepository.edgeState = appRepository.cacheEdgeState.copy(isEnableEdgeLighting = true)
                appRepository.clearCacheEdgeState()
                _restartOverlay.emit(true)
            }
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
