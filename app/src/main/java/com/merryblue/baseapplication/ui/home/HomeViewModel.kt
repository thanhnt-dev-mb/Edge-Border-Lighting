package com.merryblue.baseapplication.ui.home

import android.app.Application
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.coredata.model.edge.DisplayHole
import com.merryblue.baseapplication.coredata.model.edge.DisplayInfinity
import com.merryblue.baseapplication.coredata.model.edge.DisplayNotch
import com.merryblue.baseapplication.coredata.model.edge.DisplayNotchType
import com.merryblue.baseapplication.coredata.model.edge.EdgeSelection
import com.merryblue.baseapplication.coredata.model.edge.EdgeSettings
import com.merryblue.baseapplication.coredata.model.edge.HoleType
import com.merryblue.baseapplication.coredata.model.edge.InfinityType
import com.merryblue.baseapplication.ui.iap.BillingRepository
import com.merryblue.baseapplication.ui.view.edgelight.EdgeHoleShape
import com.merryblue.baseapplication.ui.view.edgelight.InfinityShape
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

    private var _edgeSettingsEvents = MutableSharedFlow<EdgeSettings>(replay = 0, extraBufferCapacity = 1)
    val edgeSettingsEvents = _edgeSettingsEvents.asSharedFlow()

    private var _edgeAdvancedEvents = MutableSharedFlow<Advanced>(replay = 0, extraBufferCapacity = 1)
    val edgeAdvancedEvents = _edgeAdvancedEvents.asSharedFlow()

    private var _edgeDisplayNotchTypeEvents = MutableSharedFlow<DisplayNotchType>(replay = 0, extraBufferCapacity = 1)
    val edgeDisplayNotchTypeEvents = _edgeDisplayNotchTypeEvents.asSharedFlow()

    private var _edgeHoleTypeEvents = MutableSharedFlow<EdgeHoleShape>(replay = 0, extraBufferCapacity = 1)
    val edgeHoleTypeEvents = _edgeHoleTypeEvents.asSharedFlow()

    private var _edgeInfinityTypeEvents = MutableSharedFlow<InfinityShape>(replay = 0, extraBufferCapacity = 1)
    val edgeInfinityTypeEvents = _edgeInfinityTypeEvents.asSharedFlow()

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

    fun emitEdgeSettings(event: EdgeSettings) {
        _edgeSettingsEvents.tryEmit(event)
    }

    fun emitEdgeAdvances(event: Advanced) {
        _edgeAdvancedEvents.tryEmit(event)
    }

    fun emitEdgeDisplayNotchType(event: DisplayNotchType) {
        _edgeDisplayNotchTypeEvents.tryEmit(event)
    }

    fun emitHoleType(event: EdgeHoleShape) {
        _edgeHoleTypeEvents.tryEmit(event)
    }

    fun emitInfinityType(event: InfinityShape) {
        _edgeInfinityTypeEvents.tryEmit(event)
    }
}
