package com.merryblue.baseapplication.ui.home.color

import android.app.Application
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.edge.EdgeColorItem
import com.merryblue.baseapplication.helpers.EdgeStyle.EDGE_PATTERN
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import com.merryblue.baseapplication.helpers.loadColorsFromArray
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class EdgeColorViewModel @Inject constructor(
    val app: Application,
    private val appRepository: AppRepository,
) : BaseViewModel(app) {

    private val _state = MutableStateFlow(EdgeColorState())
    val state: StateFlow<EdgeColorState> = _state.asStateFlow()

    private val _effect = Channel<EdgeColorEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val tabItemsCache = mutableMapOf<EdgeTab, List<EdgeColorItem>>()

    private val tabArrays: Map<EdgeTab, List<Int>> = mapOf(
        EdgeTab.TAB_4 to listOf(
            R.array.edge_4_1_1, R.array.edge_4_1_2, R.array.edge_4_1_3, R.array.edge_4_1_4,
            R.array.edge_4_2_1, R.array.edge_4_2_2, R.array.edge_4_2_3, R.array.edge_4_2_4,
            R.array.edge_4_3_1, R.array.edge_4_3_2, R.array.edge_4_3_3, R.array.edge_4_3_4,
        ),
        EdgeTab.TAB_3 to listOf(
            R.array.edge_3_1_1, R.array.edge_3_1_2, R.array.edge_3_1_3, R.array.edge_3_1_4,
            R.array.edge_3_2_1, R.array.edge_3_2_2, R.array.edge_3_2_3, R.array.edge_3_2_4,
            R.array.edge_3_3_1, R.array.edge_3_3_2, R.array.edge_3_3_3, R.array.edge_3_3_4
        ),
        EdgeTab.TAB_2 to listOf(
            R.array.edge_2_1_1, R.array.edge_2_1_2, R.array.edge_2_1_3, R.array.edge_2_1_4,
            R.array.edge_2_2_1, R.array.edge_2_2_2, R.array.edge_2_2_3, R.array.edge_2_2_4,
            R.array.edge_2_3_1, R.array.edge_2_3_2, R.array.edge_2_3_3, R.array.edge_2_3_4,
        )
    )

    fun dispatch(intent: EdgeColorIntent) {
        when (intent) {
            is EdgeColorIntent.LoadInitial -> loadInitialIfNeeded()
            is EdgeColorIntent.SelectTab -> reduceSelectTab(intent.tab)
            is EdgeColorIntent.SelectColor -> reduceSelectColor(intent.index)
        }
    }

    private fun loadInitialIfNeeded() {
        if (_state.value.isLoaded) return

        val saved = appRepository.edgeState

        val isPattern = saved.edgeStyleType == EDGE_PATTERN && saved.patternEnabled
        if (isPattern) {
            setTabInternal(EdgeTab.TAB_4, 0)
            markLoaded()
            emitEffectIfValid()
            return
        }

        val tab = detectTabFromColors(saved.colors)
        val itemsRaw = getItems(tab)
        val index = findIndexByColors(itemsRaw, saved.colors)

        setTabInternal(tab, index)
        markLoaded()
        emitEffectIfValid()
    }

    private fun reduceSelectTab(tab: EdgeTab) {
        setTabInternal(tab, 0)
        emitEffectIfValid()
    }

    private fun reduceSelectColor(index: Int) {
        val current = _state.value
        if (index !in current.items.indices) return
        if (index == current.selectedIndex) return

        val items = current.items.mapIndexed { i, item ->
            item.copy(isSelected = i == index)
        }

        _state.update {
            it.copy(items = items, selectedIndex = index)
        }

        emitEffectIfValid()
    }

    private fun setTabInternal(tab: EdgeTab, index: Int) {
        val raw = getItems(tab)
        val safeIndex = index.coerceIn(0, (raw.size - 1).coerceAtLeast(0))

        val items = raw.mapIndexed { i, item ->
            item.copy(isSelected = i == safeIndex)
        }

        _state.update {
            it.copy(
                selectedTab = tab,
                items = items,
                selectedIndex = if (items.isEmpty()) -1 else safeIndex
            )
        }
    }

    private fun getItems(tab: EdgeTab): List<EdgeColorItem> =
        tabItemsCache.getOrPut(tab) {
            tabArrays[tab].orEmpty().map {
                EdgeColorItem(app.applicationContext.loadColorsFromArray(it))
            }
        }

    private fun markLoaded() {
        _state.update { it.copy(isLoaded = true) }
    }

    private fun detectTabFromColors(colors: IntArray): EdgeTab =
        when (colors.size) {
            2 -> EdgeTab.TAB_2
            3 -> EdgeTab.TAB_3
            else -> EdgeTab.TAB_4
        }

    private fun findIndexByColors(items: List<EdgeColorItem>, target: IntArray): Int {
        val idx = items.indexOfFirst { it.colors.contentEquals(target) }
        return if (idx >= 0) idx else 0
    }

    private fun emitEffectIfValid() {
        val state = _state.value
        if (state.selectedIndex !in state.items.indices) return

        val colors = state.items[state.selectedIndex].colors
        viewModelScope.launch {
            _effect.send(EdgeColorEffect.ApplyColors(colors))
        }
    }

    fun applyColorsToSystem(colors: IntArray) {
        appRepository.edgeState = appRepository.edgeState.copy(colors = colors)
        val ctx = app.applicationContext
        ctx.sendBroadcast(Intent(ACTION_EDGE_OVERLAY_CHANGED).setPackage(ctx.packageName))
    }
}
