package com.merryblue.baseapplication.ui.home.advanced

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.coredata.model.edge.EdgeAdvanced
import com.merryblue.baseapplication.ui.view.edgelight.EdgeLightingState
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EdgeAdvancedViewModel @Inject constructor(
    private val appRepository: AppRepository,
    @ApplicationContext private val appContext: Context
): ViewModel() {
    private var _directionStateFlow = MutableStateFlow(EdgeAdvancedState())
    val directionStateFlow = _directionStateFlow.asStateFlow()

    private var _notchTypeStateFlow = MutableStateFlow(EdgeAdvancedState())
    val notchTypeStateFlow = _notchTypeStateFlow.asStateFlow()

    private val listDirection = buildList {
        add(EdgeAdvanced.EdgeDirection(Advanced.DIRECTION_CLOCKWISE, R.string.txt_clockwise_default, R.drawable.ic_clockwise, false))
        add(EdgeAdvanced.EdgeDirection(Advanced.DIRECTION_ANTI_CLOCKWISE, R.string.txt_anti_clockwise_default, R.drawable.ic_anti_clockwise, false))
        add(EdgeAdvanced.EdgeDirection(Advanced.DIRECTION_TOP_RIGHT_BOTTOM_LEFT, R.string.txt_top_right_to_bottom_left, R.drawable.ic_top_right_bottom_left, false))
        add(EdgeAdvanced.EdgeDirection(Advanced.DIRECTION_BOTTOM_LEFT_TOP_RIGHT, R.string.txt_bottom_left_to_top_right, R.drawable.ic_bottom_left_top_right, false))
        add(EdgeAdvanced.EdgeDirection(Advanced.DIRECTION_DOWN, R.string.txt_top_to_bottom_down, R.drawable.ic_top_to_bottom_down, false))
        add(EdgeAdvanced.EdgeDirection(Advanced.DIRECTION_UP, R.string.txt_bottom_to_top_up, R.drawable.ic_bottom_to_top_up, false))
    }

    private val listNotchType = buildList {
        add(EdgeAdvanced.EdgeNotchType(Advanced.NOTCH_DEFAULT, R.string.txt_default, R.drawable.ic_notch_type_default, false))
        add(EdgeAdvanced.EdgeNotchType(Advanced.NOTCH_DISPLAY_NOTCH, R.string.txt_display_notch, R.drawable.ic_notch_type_display_notch, false))
        add(EdgeAdvanced.EdgeNotchType(Advanced.NOTCH_DISPLAY_HOLE, R.string.txt_display_hole, R.drawable.ic_notch_type_display_hole, false))
        add(EdgeAdvanced.EdgeNotchType(Advanced.NOTCH_DISPLAY_INFINITY, R.string.txt_display_infinity, R.drawable.ic_notch_type_display_infinity, false))
    }

    private fun findDirectionIndex(type: Advanced): Int {
        val items = listDirection
        val idx = items.indexOfFirst { it.type == type }
        return if (idx >= 0) idx else 0
    }

    private fun findNotchTypeIndex(type: Advanced): Int {
        val items = listNotchType
        val idx = items.indexOfFirst { it.type == type }
        return if (idx >= 0) idx else 0
    }

    fun selectDirection(index: Int) {
        val safeIndex = index.coerceIn(0, listDirection.size - 1).coerceAtLeast(0)
        val items = listDirection.mapIndexed { pos, item ->
            item.copy(isSelected = pos == safeIndex)
        }
        _directionStateFlow.update { it.copy(directionSelectedIndex = index, listDirection = items) }
    }

    fun selectNotchType(index: Int) {
        val safeIndex = index.coerceIn(0, listNotchType.size - 1).coerceAtLeast(0)
        val items = listNotchType.mapIndexed { pos, item ->
            item.copy(isSelected = pos == safeIndex)
        }
        _notchTypeStateFlow.update { it.copy(notchTypeSelectedIndex = index, listNotchType = items) }
    }

    fun loadInitialFromSaved() {
        val s = appRepository.edgeState
        val directionIndex = findDirectionIndex(s.direction)
        val notchTypeIndex = findNotchTypeIndex(s.notchType)
        selectDirection(directionIndex)
        selectNotchType(notchTypeIndex)
    }

    fun updateEdgeState(block: (EdgeLightingState) -> EdgeLightingState) {
        appRepository.edgeState = block.invoke(appRepository.edgeState)
        val i = Intent(ACTION_EDGE_OVERLAY_CHANGED).apply {
            setPackage(appContext.packageName)
        }
        appContext.sendBroadcast(i)
    }

    fun getEdgeState() = appRepository.edgeState
}