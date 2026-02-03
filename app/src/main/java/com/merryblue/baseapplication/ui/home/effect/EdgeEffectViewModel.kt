package com.merryblue.baseapplication.ui.home.effect

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.edge.EdgeEffectItem
import com.merryblue.baseapplication.helpers.EdgeStyle.EDGE_PATTERN
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeLightingState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EdgeEffectViewModel @Inject constructor(
    private val appRepository: AppRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(EdgeEffectState())
    val state: StateFlow<EdgeEffectState> = _state.asStateFlow()

    private val _effect = Channel<EdgeEffectEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val listIconRes = buildList {
        add(EdgeEffectItem(R.drawable.ic_none, false))
        add(EdgeEffectItem(R.drawable.ic_love, false))
        add(EdgeEffectItem(R.drawable.ic_circle, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_moon, false))
        add(EdgeEffectItem(R.drawable.ic_sunshine, false))
        add(EdgeEffectItem(R.drawable.ic_butterfly, false))
        add(EdgeEffectItem(R.drawable.ic_cloud, false))
        add(EdgeEffectItem(R.drawable.ic_bird, false))
        add(EdgeEffectItem(R.drawable.ic_flower, false))
        add(EdgeEffectItem(R.drawable.ic_snow, false))
        add(EdgeEffectItem(R.drawable.ic_cat, false))
        add(EdgeEffectItem(R.drawable.ic_star_fall, false))
        add(EdgeEffectItem(R.drawable.ic_pet_paw, false))
        add(EdgeEffectItem(R.drawable.ic_flower_fill, false))
        add(EdgeEffectItem(R.drawable.ic_tulip, false))
        add(EdgeEffectItem(R.drawable.ic_tennis_ball, false))
        add(EdgeEffectItem(R.drawable.ic_emoji_smile, false))
        add(EdgeEffectItem(R.drawable.ic_emoji_joy, false))
        add(EdgeEffectItem(R.drawable.ic_emoji_bomb, false))
    }

    fun dispatch(intent: EdgeEffectIntent) {
        when (intent) {
            is EdgeEffectIntent.LoadInitial -> loadInitialIfNeeded()
            is EdgeEffectIntent.SelectEffect -> reduceSelectEffect(intent.index)
            is EdgeEffectIntent.UpdateSize -> updateEdgeStateAndBroadcast { it.copy(iconSizePx = intent.sizePx) }
            is EdgeEffectIntent.UpdateSpeed -> updateEdgeStateAndBroadcast { it.copy(speedMs = intent.speedMs) }
            is EdgeEffectIntent.UpdateBottomRadius -> updateEdgeStateAndBroadcast { it.copy(bottomRadius = intent.radiusPx) }
            is EdgeEffectIntent.UpdateTopRadius -> updateEdgeStateAndBroadcast { it.copy(topRadius = intent.radiusPx) }
        }
    }

    private fun loadInitialIfNeeded() {
        if (_state.value.isLoaded) return

        val savedResId = appRepository.edgeState.vectorResId
        val index = findEffectIndexByResId(savedResId)

        setEffectInternal(index)
        _state.update { it.copy(isLoaded = true) }
    }

    private fun reduceSelectEffect(index: Int) {
        setEffectInternal(index)

        val items = _state.value.listEffect
        if (index in items.indices) {
            val selected = items[index]
            updateEdgeStateAndBroadcast {
                it.copy(edgeStyleType = EDGE_PATTERN, vectorResId = selected.resId)
            }
        }
    }

    private fun setEffectInternal(index: Int) {
        val safeIndex = index.coerceIn(0, listIconRes.size - 1).coerceAtLeast(0)
        val items = listIconRes.mapIndexed { pos, item ->
            item.copy(isSelected = pos == safeIndex)
        }

        _state.update {
            it.copy(
                selectedIndex = if (items.isEmpty()) -1 else safeIndex,
                listEffect = items
            )
        }
    }

    private fun findEffectIndexByResId(resId: Int): Int {
        if (resId == 0) return 0
        val idx = listIconRes.indexOfFirst { it.resId == resId }
        return if (idx >= 0) idx else 0
    }

    private fun updateEdgeStateAndBroadcast(block: (EdgeLightingState) -> EdgeLightingState) {
        appRepository.edgeState = block.invoke(appRepository.edgeState)
        val intent = Intent(ACTION_EDGE_OVERLAY_CHANGED).apply {
            setPackage(appContext.packageName)
        }
        appContext.sendBroadcast(intent)
    }

    fun getEdgeState(): EdgeLightingState = appRepository.edgeState
}