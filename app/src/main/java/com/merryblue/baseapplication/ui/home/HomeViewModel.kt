package com.merryblue.baseapplication.ui.home

import android.app.Application
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.edge.EdgePreset
import com.merryblue.baseapplication.coredata.model.edge.EdgeSelection
import com.merryblue.baseapplication.ui.iap.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val appRepository: AppRepository,
    private val billingRepository: BillingRepository
) : BaseViewModel(application) {

    private val _edgeColorEvents = MutableSharedFlow<EdgeSelection>(replay = 0, extraBufferCapacity = 1)
    val edgeColorEvents = _edgeColorEvents.asSharedFlow()

    val connectionState = appRepository.networkState
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    var isStartSession
        get() = appRepository.isStartSession
        set(value) {
            appRepository.isStartSession = value
        }

    val serviceRunning: Boolean
        get() = appRepository.isServiceRunning

    val lockedAppCount: Int
        get() = appRepository.lockedAppCount

    fun isPremium() = billingRepository.isPurchased()

    fun getRemoteConfiguration() = appRepository.loadAdsConfiguration()

    fun isRated() = appRepository.rated >= 4

    fun setRate(rate: Int) {
        appRepository.rated = rate
    }

    fun emitEdgeColor(event: EdgeSelection) {
        _edgeColorEvents.tryEmit(event)
    }
}
