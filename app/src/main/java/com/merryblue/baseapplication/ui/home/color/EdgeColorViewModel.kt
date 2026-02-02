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
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeLightingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class EdgeColorViewModel @Inject constructor(
    val app: Application,
    private val appRepository: AppRepository,
) : BaseViewModel(app) {

    private val tabItemsCache = mutableMapOf<EdgeTab, List<EdgeColorItem>>()

    private val _state = MutableSharedFlow<EdgeColorState>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val state = _state.asSharedFlow()

    private var currentState = EdgeColorState()

    private val tabArrays: Map<EdgeTab, List<Int>> = mapOf(
        EdgeTab.TAB_4 to listOf(
            R.array.edge_4_1_1,
            R.array.edge_4_1_2,
            R.array.edge_4_1_3,
            R.array.edge_4_1_4,
            R.array.edge_4_2_1,
            R.array.edge_4_2_2,
            R.array.edge_4_2_3,
            R.array.edge_4_2_4,
            R.array.edge_4_3_1,
            R.array.edge_4_3_2,
            R.array.edge_4_3_3,
            R.array.edge_4_3_4,
        ),

        EdgeTab.TAB_3 to listOf(
            R.array.edge_3_1_1,
            R.array.edge_3_1_2,
            R.array.edge_3_1_2,
            R.array.edge_3_1_2,
            R.array.edge_3_1_2,
            R.array.edge_3_1_2,
            R.array.edge_3_1_2,
            R.array.edge_3_1_2,
            R.array.edge_3_1_2,
            R.array.edge_3_1_2,
            R.array.edge_3_1_2,
            R.array.edge_3_1_2,
        ),

        EdgeTab.TAB_2 to listOf(
            R.array.edge_2_1_1,
            R.array.edge_2_1_2,
            R.array.edge_2_1_2,
            R.array.edge_2_1_2,
            R.array.edge_2_1_2,
            R.array.edge_2_1_2,
            R.array.edge_2_1_2,
            R.array.edge_2_1_2,
            R.array.edge_2_1_2,
            R.array.edge_2_1_2,
            R.array.edge_2_1_2,
            R.array.edge_2_1_2,
        ),
    )

    private fun setTab(tab: EdgeTab, defaultIndex: Int) {
        val itemsRaw = tabItemsCache.getOrPut(tab) {
            val arrayIds = tabArrays[tab].orEmpty()
            arrayIds.map { EdgeColorItem(app.applicationContext.loadColorsFromArray(it)) }
        }

        val safeIndex = defaultIndex.coerceIn(0, (itemsRaw.size - 1).coerceAtLeast(0))
        val items = itemsRaw.mapIndexed { index, item ->
            item.copy(isSelected = index == safeIndex)
        }

        currentState = currentState.copy(
            selectedTab = tab,
            items = items,
            selectedIndex = if (items.isEmpty()) -1 else safeIndex
        )

        viewModelScope.launch {
            _state.emit(currentState)
        }
    }

    private fun selectColor(newIndex: Int) {
        if (newIndex == currentState.selectedIndex) return
        if (newIndex !in currentState.items.indices) return

        val newItems = currentState.items.mapIndexed { index, item ->
            item.copy(isSelected = index == newIndex)
        }

        currentState = currentState.copy(
            items = newItems,
            selectedIndex = newIndex
        )

        viewModelScope.launch {
            _state.emit(currentState)
        }
    }

    private fun detectTabFromColors(colors: IntArray): EdgeTab {
        return when (colors.size) {
            2 -> EdgeTab.TAB_2
            3 -> EdgeTab.TAB_3
            else -> EdgeTab.TAB_4
        }
    }

    private fun findIndexByColors(items: List<EdgeColorItem>, target: IntArray): Int {
        val idx = items.indexOfFirst { it.colors.contentEquals(target) }
        return if (idx >= 0) idx else 0
    }

    fun loadInitial(defaultTab: EdgeTab = EdgeTab.TAB_4, defaultIndex: Int = 0) {
        val saved = appRepository.edgeState

        val isPattern = saved.edgeStyleType == EDGE_PATTERN && saved.patternEnabled
        if (isPattern) {
            setTab(defaultTab, defaultIndex)
            return
        }

        val savedColors = saved.colors
        val tab = detectTabFromColors(savedColors)

        val itemsRaw = tabItemsCache.getOrPut(tab) {
            val arrayIds = tabArrays[tab].orEmpty()
            arrayIds.map { EdgeColorItem(app.applicationContext.loadColorsFromArray(it)) }
        }

        val idx = if (itemsRaw.isEmpty()) 0 else findIndexByColors(itemsRaw, savedColors)
        setTab(tab, idx)
    }

    fun dispatch(intent: EdgeColorIntent) {
        when (intent) {
            is EdgeColorIntent.SelectTab -> setTab(intent.tab, defaultIndex = 0)
            is EdgeColorIntent.SelectColor -> selectColor(intent.index)
        }
    }

    fun updateEdgeState(block: (EdgeLightingState) -> EdgeLightingState) {
        appRepository.edgeState = block.invoke(appRepository.edgeState)
        val ctx = app.applicationContext
        val i = Intent(ACTION_EDGE_OVERLAY_CHANGED).apply {
            setPackage(ctx.packageName)
        }
        ctx.sendBroadcast(i)
    }
}
