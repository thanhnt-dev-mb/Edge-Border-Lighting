package com.merryblue.baseapplication.ui.home.color

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.edge.EdgeColorItem
import com.merryblue.baseapplication.ui.home.EdgeLightingState
import com.merryblue.baseapplication.helpers.ACTION_EDGE_STATE_CHANGED
import com.merryblue.baseapplication.helpers.loadColorsFromArray
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class EdgeColorViewModel @Inject constructor(
    val app: Application,
    private val appRepository: AppRepository,
) : BaseViewModel(app) {

    private val tabItemsCache = mutableMapOf<EdgeTab, List<EdgeColorItem>>()

    private val _state = MutableStateFlow(EdgeColorState())
    val state: StateFlow<EdgeColorState> = _state.asStateFlow()

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

        _state.value = _state.value.copy(
            selectedTab = tab,
            items = items,
            selectedIndex = if (items.isEmpty()) -1 else safeIndex
        )
    }

    private fun selectColor(newIndex: Int) {
        val current = _state.value
        if (newIndex == current.selectedIndex) return
        if (newIndex !in current.items.indices) return

        val newItems = current.items.mapIndexed { index, item ->
            item.copy(isSelected = index == newIndex)
        }

        _state.value = current.copy(
            items = newItems,
            selectedIndex = newIndex
        )
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

        // If pattern style is active, color screen is not applicable -> fallback to default
        val isPattern = saved.edgeStyleType == 1 && saved.patternEnabled
        if (isPattern) {
            setTab(defaultTab, defaultIndex)
            return
        }

        val savedColors = saved.colors
        val tab = detectTabFromColors(savedColors)

        // Load raw items first to resolve the selected index
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
        val i = Intent(ACTION_EDGE_STATE_CHANGED).apply {
            setPackage(ctx.packageName)
        }
        ctx.sendBroadcast(i)
    }
}
