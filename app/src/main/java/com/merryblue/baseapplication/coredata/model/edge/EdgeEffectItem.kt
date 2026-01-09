package com.merryblue.baseapplication.coredata.model.edge

import androidx.annotation.DrawableRes

data class EdgeEffectItem(
    @DrawableRes val resId: Int,
    var isSelected: Boolean
)