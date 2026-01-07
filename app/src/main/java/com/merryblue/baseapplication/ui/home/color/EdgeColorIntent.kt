package com.merryblue.baseapplication.ui.home.color

sealed interface EdgeColorIntent {
    data class SelectColor(val index: Int) : EdgeColorIntent
}
