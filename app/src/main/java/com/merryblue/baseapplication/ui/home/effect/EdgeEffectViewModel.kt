package com.merryblue.baseapplication.ui.home.effect

import androidx.lifecycle.ViewModel
import com.merryblue.baseapplication.Application
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.edge.EdgeEffectItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class EdgeEffectViewModel @Inject constructor(): ViewModel() {
    private var _state = MutableStateFlow(EdgeEffectState())
    val state = _state.asStateFlow()

    private val listIconRes = buildList {
        add(EdgeEffectItem(R.drawable.ic_none, true))
        add(EdgeEffectItem(R.drawable.ic_love, false))
        add(EdgeEffectItem(R.drawable.ic_circle, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
        add(EdgeEffectItem(R.drawable.ic_star, false))
    }

    fun loadEffect(index: Int) {
        val safeIndex = index.coerceIn(0, listIconRes.size - 1).coerceAtLeast(0)
        val items = listIconRes.mapIndexed { pos, item ->
            item.copy(isSelected = pos == safeIndex)
        }
        _state.value = _state.value.copy(
            selectedIndex = if (items.isEmpty()) -1 else safeIndex,
            listEffect = items
        )
    }
}