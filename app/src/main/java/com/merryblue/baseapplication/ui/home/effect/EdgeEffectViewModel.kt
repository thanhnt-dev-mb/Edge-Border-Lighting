package com.merryblue.baseapplication.ui.home.effect

import androidx.lifecycle.ViewModel
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.edge.EdgeEffectItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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