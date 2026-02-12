package com.merryblue.baseapplication.ui.home.advanced

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.coredata.model.edge.EdgeAdvanced
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeHoleShape
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeLightingState
import com.merryblue.baseapplication.ui.view.edgelight.model.InfinityShape
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EdgeAdvancedViewModel @Inject constructor(
    private val appRepository: AppRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(EdgeAdvancedState())
    val state: StateFlow<EdgeAdvancedState> = _state.asStateFlow()

    private val _effect = Channel<EdgeAdvancedEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

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

    fun dispatch(intent: EdgeAdvancedIntent) {
        when (intent) {
            is EdgeAdvancedIntent.LoadInitial -> loadInitialIfNeeded()
            is EdgeAdvancedIntent.SelectDirection -> reduceSelectDirection(intent.index)
            is EdgeAdvancedIntent.SelectNotchType -> reduceSelectNotchType(intent.index)

            // Notch
            is EdgeAdvancedIntent.UpdateNotchWidth -> updateEdgeStateAndBroadcast { it.copy(notchWidthFraction = intent.fraction) }
            is EdgeAdvancedIntent.UpdateNotchHeight -> updateEdgeStateAndBroadcast { it.copy(notchHeightPx = intent.heightPx) }
            is EdgeAdvancedIntent.UpdateNotchTopRadius -> updateEdgeStateAndBroadcast { it.copy(notchTopRadiusPx = intent.radiusPx) }
            is EdgeAdvancedIntent.UpdateNotchBottomRadius -> updateEdgeStateAndBroadcast { it.copy(notchBottomRadiusPx = intent.radiusPx) }
            is EdgeAdvancedIntent.UpdateNotchBottomFullness -> updateEdgeStateAndBroadcast { it.copy(notchBottomFullness = intent.fullness) }

            // Hole
            is EdgeAdvancedIntent.SelectHoleShape -> reduceSelectHoleShape(intent.shape)
            is EdgeAdvancedIntent.UpdateHoleOffsetX -> updateEdgeStateAndBroadcast { it.copy(holeOffsetX = intent.offset) }
            is EdgeAdvancedIntent.UpdateHoleOffsetY -> updateEdgeStateAndBroadcast { it.copy(holeOffsetY = intent.offset) }
            is EdgeAdvancedIntent.UpdateHoleRadius -> updateEdgeStateAndBroadcast { it.copy(holeRadius = intent.radius) }
            is EdgeAdvancedIntent.UpdateHoleWidth -> updateEdgeStateAndBroadcast { it.copy(holeWidthPx = intent.widthPx) }
            is EdgeAdvancedIntent.UpdateHoleHeight -> updateEdgeStateAndBroadcast { it.copy(holeHeightPx = intent.heightPx) }
            is EdgeAdvancedIntent.UpdateHoleCornerRadius -> updateEdgeStateAndBroadcast { it.copy(holeCornerRadiusPx = intent.radiusPx) }

            // Infinity
            is EdgeAdvancedIntent.SelectInfinityShape -> reduceSelectInfinityShape(intent.shape)
            is EdgeAdvancedIntent.UpdateInfinityWidth -> updateEdgeStateAndBroadcast { it.copy(infinityWidthPx = intent.widthPx) }
            is EdgeAdvancedIntent.UpdateInfinityHeight -> updateEdgeStateAndBroadcast { it.copy(infinityHeightPx = intent.heightPx) }
            is EdgeAdvancedIntent.UpdateInfinityTopRadius -> updateEdgeStateAndBroadcast { it.copy(infinityRadiusTopPx = intent.radiusPx) }
        }
    }

    private fun loadInitialIfNeeded() {
        if (_state.value.isLoaded) return

        val s = appRepository.edgeState
        val directionIndex = findDirectionIndex(s.direction)
        val notchTypeIndex = findNotchTypeIndex(s.notchType)

        setDirectionInternal(directionIndex)
        setNotchTypeInternal(notchTypeIndex)

        _state.update {
            it.copy(
                currentNotchType = s.notchType,
                currentHoleShape = s.holeShape,
                currentInfinityShape = s.infinityShape,
                isLoaded = true
            )
        }
    }

    private fun reduceSelectDirection(index: Int) {
        setDirectionInternal(index)

        val items = _state.value.listDirection
        if (index in items.indices) {
            updateEdgeStateAndBroadcast { it.copy(direction = items[index].type) }
        }
    }

    private fun reduceSelectNotchType(index: Int) {
        setNotchTypeInternal(index)

        val items = _state.value.listNotchType
        if (index in items.indices) {
            val selected = items[index]
            updateEdgeStateAndBroadcast { it.copy(notchType = selected.type) }
            _state.update { it.copy(currentNotchType = selected.type) }
            emitRefreshLayout()
        }
    }

    private fun reduceSelectHoleShape(shape: EdgeHoleShape) {
        _state.update { it.copy(currentHoleShape = shape) }
        updateEdgeStateAndBroadcast { it.copy(holeShape = shape) }
        emitRefreshLayout()
    }

    private fun reduceSelectInfinityShape(shape: InfinityShape) {
        _state.update { it.copy(currentInfinityShape = shape) }
        updateEdgeStateAndBroadcast { it.copy(infinityShape = shape) }
        emitRefreshLayout()
    }

    private fun setDirectionInternal(index: Int) {
        val safeIndex = index.coerceIn(0, listDirection.size - 1).coerceAtLeast(0)
        val items = listDirection.mapIndexed { pos, item ->
            item.copy(isSelected = pos == safeIndex)
        }
        _state.update {
            it.copy(directionSelectedIndex = safeIndex, listDirection = items)
        }
    }

    private fun setNotchTypeInternal(index: Int) {
        val safeIndex = index.coerceIn(0, listNotchType.size - 1).coerceAtLeast(0)
        val items = listNotchType.mapIndexed { pos, item ->
            item.copy(isSelected = pos == safeIndex)
        }
        _state.update {
            it.copy(notchTypeSelectedIndex = safeIndex, listNotchType = items)
        }
    }

    private fun findDirectionIndex(type: Advanced): Int {
        val idx = listDirection.indexOfFirst { it.type == type }
        return if (idx >= 0) idx else 0
    }

    private fun findNotchTypeIndex(type: Advanced): Int {
        val idx = listNotchType.indexOfFirst { it.type == type }
        return if (idx >= 0) idx else 0
    }

    private fun updateEdgeStateAndBroadcast(block: (EdgeLightingState) -> EdgeLightingState) {
        appRepository.edgeState = block.invoke(appRepository.edgeState)
        val intent = Intent(ACTION_EDGE_OVERLAY_CHANGED).apply {
            setPackage(appContext.packageName)
        }
        appContext.sendBroadcast(intent)
    }

    private fun emitRefreshLayout() {
        viewModelScope.launch {
            _effect.send(EdgeAdvancedEffect.RefreshLayout)
        }
    }

    fun getEdgeState() = appRepository.edgeState
}