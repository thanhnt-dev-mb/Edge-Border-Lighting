package com.merryblue.baseapplication.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.ui.iap.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val appRepository: AppRepository,
    private val billingRepository: BillingRepository
) : BaseViewModel(application) {

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
}
