package com.merryblue.baseapplication.ui.home.color

import androidx.lifecycle.ViewModel
import com.merryblue.baseapplication.coredata.model.EdgeColorItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EdgeColorViewModel : ViewModel() {

    private val _state = MutableStateFlow(EdgeColorState())
    val state: StateFlow<EdgeColorState> = _state.asStateFlow()

    fun dispatch(intent: EdgeColorIntent) {
        when (intent) {
            is EdgeColorIntent.SelectColor -> reduceSelect(intent.index)
        }
    }

    private fun reduceSelect(newIndex: Int) {
        val current = _state.value
        if (newIndex == current.selectedIndex) return

        val newItems = current.items.mapIndexed { index, item ->
            item.copy(isSelected = index == newIndex)
        }

        _state.value = current.copy(
            items = newItems,
            selectedIndex = newIndex
        )
    }

    fun setInitialData(items: List<EdgeColorItem>, defaultIndex: Int = 0) {
        val initItems = items.mapIndexed { index, item ->
            item.copy(isSelected = index == defaultIndex)
        }

        _state.value = EdgeColorState(
            items = initItems,
            selectedIndex = defaultIndex
        )
    }
}
