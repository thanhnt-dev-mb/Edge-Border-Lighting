package com.merryblue.baseapplication.ui.wallpaper

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.repository.EdgeDataRepository
import com.merryblue.baseapplication.domain.repository.EdgeImageRepository
import com.merryblue.baseapplication.domain.repository.EdgeImageSource
import com.merryblue.baseapplication.domain.repository.TargetSize
import com.merryblue.baseapplication.helpers.BackgroundType
import com.merryblue.baseapplication.helpers.EdgeStyle.EDGE_LINEAR
import com.merryblue.baseapplication.helpers.EdgeStyle.EDGE_NONE
import com.merryblue.baseapplication.helpers.PreviewType.EDGE_WALLPAPER_SCREEN
import com.merryblue.baseapplication.helpers.PreviewType.RIPPLE_WALLPAPER_SCREEN
import com.merryblue.baseapplication.helpers.PreviewType.STATIC_WALLPAPER_SCREEN
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_STOP
import com.merryblue.baseapplication.helpers.WallpaperType
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.ui.theme.ThemePagingSource
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeLightingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    private val application: Application,
    private val repo: EdgeDataRepository,
    private val edgeImageRepository: EdgeImageRepository,
    private val appPreferences: AppPreferences,
    private val appRepository: AppRepository
) : BaseViewModel(application) {

    var videoUrl: String
        get() = appRepository.videoUrl
        set(value) { appRepository.videoUrl = value }

    var rippleEffectUrl: String
        get() = appRepository.rippleEffectUrl
        set(value) { appRepository.rippleEffectUrl = value }

    var edgeState: EdgeLightingState
        get() = appRepository.edgeState
        set(value) { appRepository.edgeState = value }

    val connectionState = appRepository.networkState

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

    fun canSetLive() : Boolean {
        return appPreferences.canChangeLive || appPreferences.canLiveChooser
    }

    fun getPaging(type: String, isGallery: Boolean, isCustom: Boolean) = Pager(PagingConfig(pageSize = 15, enablePlaceholders = false)) {
        ThemePagingSource(type, isGallery, isCustom, repo)
    }.flow.cachedIn(viewModelScope)

    fun loadEdgeBackgroundUrl(item: Item, target: TargetSize, onCompleted: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            edgeState = getPresetEdgeLighting(item)
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.Url(item.pathUrl), target)
            bmp?.let { sendActionBroadcast(ACTION_EDGE_OVERLAY_CHANGED) }
            onCompleted.invoke(bmp)
        }
    }

    fun loadStaticBackgroundUrl(item: Item, target: TargetSize, onCompleted: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            if (item.type == WallpaperType.TYPE_EDGE) edgeState = getPresetEdgeLighting(item)

            val originalUrl = if (item.type == WallpaperType.TYPE_VIDEO) item.thumbUrl else item.pathUrl
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.Url(originalUrl), target)
            bmp?.let { sendActionBroadcast(ACTION_EDGE_OVERLAY_CHANGED) }
            onCompleted.invoke(bmp)
        }
    }

    fun loadBackgroundRippleUrl(item: Item, target: TargetSize, onCompleted: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.Url(item.pathUrl), target)
            onCompleted.invoke(bmp)
        }
    }

    fun loadBackgroundUri(uri: Uri, target: TargetSize, onCompleted: (String, Bitmap?) -> Unit) {
        val newState = edgeState.copy(
            backgroundType = BackgroundType.BACKGROUND_URI,
            backgroundImageUriString = uri.toString(),
            backgroundImageUrl = null,
            backgroundImageResId = 0
        )
        edgeState = newState

        viewModelScope.launch {
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.UriSource(uri), target)
            val key = if (edgeState.isEnableEdgeLighting) EDGE_WALLPAPER_SCREEN else STATIC_WALLPAPER_SCREEN
            onCompleted.invoke(key, bmp)
        }
    }

    fun updateEdgeState(block: (EdgeLightingState) -> EdgeLightingState) {
        edgeState = block.invoke(edgeState)
        sendActionBroadcast(ACTION_EDGE_OVERLAY_CHANGED)
    }

    fun sendActionBroadcast(action: String) {
        val ctx = application.applicationContext
        val i = Intent(action).apply {
            setPackage(ctx.packageName)
        }
        ctx.sendBroadcast(i)
    }

}