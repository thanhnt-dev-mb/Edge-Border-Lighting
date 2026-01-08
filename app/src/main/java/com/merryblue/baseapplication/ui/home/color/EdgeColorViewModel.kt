package com.merryblue.baseapplication.ui.home.color

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.edge.EdgeColorItem
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

    fun loadInitial(tab: EdgeTab = EdgeTab.TAB_4, defaultIndex: Int = 0) {
        setTab(tab, defaultIndex)
    }

    fun dispatch(intent: EdgeColorIntent) {
        when (intent) {
            is EdgeColorIntent.SelectTab -> setTab(intent.tab, defaultIndex = 0)
            is EdgeColorIntent.SelectColor -> selectColor(intent.index)
        }
    }
}
