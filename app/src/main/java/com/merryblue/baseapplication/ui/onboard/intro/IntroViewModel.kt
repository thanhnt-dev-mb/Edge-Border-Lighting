package com.merryblue.baseapplication.ui.onboard.intro

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.IntroModel
import com.merryblue.baseapplication.enums.IntroPage
import com.merryblue.baseapplication.ui.iap.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.app.core.ads.remoteconfig.CoreRemoteConfig
import org.app.core.base.BaseViewModel
import org.app.core.base.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    application: Application,
    private val appRepository: AppRepository,
    private val billingRepository: BillingRepository
) : BaseViewModel(application) {

    val currentPage = MutableLiveData<Int>()
    val openHomeEvent = SingleLiveEvent<Boolean>()

    val connectionState = appRepository.networkState

    fun getRemoteConfiguration() = appRepository.loadAdsConfiguration()

    fun isPremium() = billingRepository.isPurchased()

    fun setCurrentPage(page: Int) {
        currentPage.postValue(page)
    }

    fun getPageDataBy(index: Int, context: Context) : IntroModel {
        val hideAds = hideNativeFullPage()
        return IntroPage.pageDataBy(index, context, hideAds)
    }

    fun goNextByPage(page: Int) {
        val hideAds = hideNativeFullPage()
        val size = IntroPage.allPage(hideAds).size
        if (page < (size - 1)) {
            currentPage.postValue(page + 1)
        } else {
            openHomeEvent.postValue(true)
        }
    }
    
    fun setFirstTime(isFirstTime: Boolean) {
        appRepository.isFirstLaunch = isFirstTime
    }

    fun hideNativeFullPage() : Boolean {
        if (billingRepository.isPurchased()) return true

        val remoteConfig = appRepository.loadAdsConfiguration()
        remoteConfig ?: return true

        val tagNative = "NativeFullscreenFragment_Native"
        val nativeAds = remoteConfig.natives?.firstOrNull {
            it.tag == tagNative && !it.id.isNullOrBlank()
        }

        return nativeAds == null
    }
}
