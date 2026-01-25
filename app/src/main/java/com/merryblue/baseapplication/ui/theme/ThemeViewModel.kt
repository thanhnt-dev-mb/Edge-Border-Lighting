package com.merryblue.baseapplication.ui.theme

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Color.parseColor
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.repository.EdgeDataRepository
import com.merryblue.baseapplication.domain.repository.EdgeImageRepository
import com.merryblue.baseapplication.domain.repository.EdgeImageSource
import com.merryblue.baseapplication.domain.repository.TargetSize
import com.merryblue.baseapplication.helpers.BackgroundType
import com.merryblue.baseapplication.helpers.EdgeStyle.EDGE_LINEAR
import com.merryblue.baseapplication.helpers.EdgeStyle.EDGE_NONE
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_STOP
import com.merryblue.baseapplication.ui.view.edgelight.EdgeLightingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val application: Application,
    private val repo: EdgeDataRepository,
    private val edgeImageRepository: EdgeImageRepository,
    private val appPreferences: AppPreferences,
) : BaseViewModel(application) {

    private val _bgBitmap = MutableSharedFlow<Bitmap?>(replay = 0)
    val bgBitmap = _bgBitmap.asSharedFlow()

    fun getPaging(type: String) = Pager(PagingConfig(pageSize = 15, enablePlaceholders = false)) {
        ThemePagingSource(type, repo)
    }.flow.cachedIn(viewModelScope)

    fun onClickBackgroundUrl(item: Item, target: TargetSize) {

        appPreferences.cacheEdgeState = appPreferences.edgeState

        val colorsInt = item.colors?.map(Color::parseColor)?.toIntArray()
        val newState = appPreferences.edgeState.copy(
            edgeStyleType = if (colorsInt != null) EDGE_LINEAR else EDGE_NONE,
            backgroundType = BackgroundType.BACKGROUND_URL,
            backgroundImageUrl = item.pathUrl,
            backgroundImageUriString = null,
            backgroundImageResId = 0,
            colors = colorsInt ?: appPreferences.edgeState.colors
        )

        appPreferences.edgeState = newState

        viewModelScope.launch {
            val bmp = edgeImageRepository.loadBitmap(EdgeImageSource.Url(item.pathUrl), target)
            bmp?.let { sendActionBroadcast(ACTION_EDGE_OVERLAY_STOP) }
            _bgBitmap.emit(bmp)
        }
    }

    fun updateEdgeState(block: (EdgeLightingState) -> EdgeLightingState) {
        appPreferences.edgeState = block.invoke(appPreferences.edgeState)
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