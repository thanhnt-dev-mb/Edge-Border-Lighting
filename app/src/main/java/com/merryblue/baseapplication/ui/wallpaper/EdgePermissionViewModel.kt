package com.merryblue.baseapplication.ui.wallpaper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EdgePermissionViewModel @Inject constructor(): ViewModel() {
    private val _edgePermission = Channel<Unit>(Channel.BUFFERED)
    val edgePermission = _edgePermission.receiveAsFlow()

    fun navigateSetting() {
        viewModelScope.launch {
            _edgePermission.send(Unit)
        }
    }
}