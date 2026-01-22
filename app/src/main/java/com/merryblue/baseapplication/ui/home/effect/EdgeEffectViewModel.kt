package com.merryblue.baseapplication.ui.home.effect

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.edge.EdgeEffectItem
import com.merryblue.baseapplication.ui.view.edgelight.EdgeLightingState
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class EdgeEffectViewModel @Inject constructor(
    private val appRepository: AppRepository,
    @ApplicationContext private val appContext: Context
): ViewModel() {
    private var _state = MutableStateFlow(EdgeEffectState())
    val state = _state.asStateFlow()

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

    private fun findEffectIndexByResId(resId: Int): Int {
        if (resId == 0) return 0
        val idx = listIconRes.indexOfFirst { it.resId == resId }
        return if (idx >= 0) idx else 0
    }

    fun loadEffect(index: Int = 0) {
        val safeIndex = index.coerceIn(0, listIconRes.size - 1).coerceAtLeast(0)
        val items = listIconRes.mapIndexed { pos, item ->
            item.copy(isSelected = pos == safeIndex)
        }
        _state.value = _state.value.copy(
            selectedIndex = if (items.isEmpty()) -1 else safeIndex,
            listEffect = items
        )
    }

    fun loadInitialFromSaved() {
        val savedResId = appRepository.edgeState.vectorResId
        val idx = findEffectIndexByResId(savedResId)
        loadEffect(idx)
    }

    fun updateEdgeState(block: (EdgeLightingState) -> EdgeLightingState) {
        appRepository.edgeState = block.invoke(appRepository.edgeState)
        val i = Intent(ACTION_EDGE_OVERLAY_CHANGED).apply {
            setPackage(appContext.packageName)
        }
        appContext.sendBroadcast(i)
    }

    fun getEdgeState(): EdgeLightingState = appRepository.edgeState
}