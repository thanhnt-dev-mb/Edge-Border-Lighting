package com.merryblue.baseapplication.ui.splash

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.AdSize
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.enums.InterstitialFunction
import com.merryblue.baseapplication.helpers.AdsUnit
import com.merryblue.baseapplication.ui.iap.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.app.core.ads.CoreAds
import org.app.core.ads.base.NativeStyle
import org.app.core.ads.openads.AdapterOpenAppManager
import org.app.core.ads.remoteconfig.CoreRemoteConfig
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@SuppressLint("LogNotTimber")
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val appRepository: AppRepository,
    application: Application,
    private val billingRepository: BillingRepository
) : BaseViewModel(application) {

    fun isFirstTime() = appRepository.isFirstLaunch

    val connectionState = appRepository.networkState

    fun getRemoteConfiguration() = appRepository.loadAdsConfiguration()

    fun getJsonConfiguration() = appRepository.getJsonAdsConfigure()

    fun isPremium() = billingRepository.isPurchased()

    init {
        viewModelScope.launch {
            billingRepository.initialize()
        }
    }

    fun initInterAdsIfNeed(activity: SplashActivity) {
        val rmConfig = appRepository.loadAdsConfiguration() ?: return
        rmConfig.interstitials?.forEach { interstitial ->
            if (interstitial.always_preload == true &&
                !interstitial.id.isNullOrBlank()) {

                Log.i(TAG, "init remote Inter Ads: ${interstitial.id} - ${interstitial.tag}")
                CoreAds.instance.initAdapterInterstitialAds(
                    activity,
                    interstitial.id!!,
                    interstitial.event ?: "DummyEventInter",
                    1
                )
            }
        }
    }

    @SuppressLint("LogNotTimber")
    fun preloadBanner(activity: SplashActivity) {
        val rmConfig = appRepository.loadAdsConfiguration() ?: return

        rmConfig.banners?.forEach { banner ->
            if (banner.status == true &&
                !banner.id.isNullOrBlank()) {
                if (!appRepository.isFirstLaunch) {
                    if (banner.tag?.contains("LanguageFragment") == true ||
                        banner.tag?.contains("LanguageActivity") == true ||
                        banner.tag?.contains("IntroPagerFragment") == true
                    ) {
                        // Should not init onboard native
                    } else {
                        Log.i(TAG, "init remote BannerAdmob: ${banner.id} - ${banner.tag}")
                        CoreAds.instance.initAdapterBannerAds(
                            banner.id!!,
                            banner.event ?: "DummyEventBanner",
                            if (banner.size == "medium") AdSize.MEDIUM_RECTANGLE else null,
                            null,
                            1
                        )
                    }
                } else {
                    Log.i(TAG, "init remote [first time] Banner Ads: ${banner.id} - ${banner.tag}")
                    CoreAds.instance.initAdapterBannerAds(
                        banner.id!!,
                        banner.event ?: "DummyEventBanner",
                        if (banner.size == "medium") AdSize.MEDIUM_RECTANGLE else null,
                        null,
                        1
                    )
                }
            }
        }
    }

    @SuppressLint("LogNotTimber")
    fun preloadNative(activity: SplashActivity) {
        val rmConfig = appRepository.loadAdsConfiguration() ?: return

        val existedIds = mutableListOf<String>()
        rmConfig.natives?.forEach { native ->
            if (!native.id.isNullOrBlank() &&
                native.place_preload == "SplashActivity") {
                if (!existedIds.contains(native.id)) {
                    val preload = native.preload ?: 0
                    if (!appRepository.isFirstLaunch) {
                        if (native.tag?.contains("LanguageFragment") == true ||
                            native.tag?.contains("LanguageActivity") == true ||
                            native.tag?.contains("IntroPagerFragment") == true) {
                            // Should not init onboard native
                        } else {
                            Log.i(TAG, "init remote Native Ads: ${native.id} - ${native.tag} - $preload")
                            existedIds.add(native.id!!)
                            CoreAds.instance.initAdapterNativeAdsMultiple(
                                activity.applicationContext,
                                activity,
                                native.id!!,
                                native.event ?: "DummyEventNative",
                                native.style ?: NativeStyle.BIG_13,
                                preload,
                                false
                            )
                        }
                    } else {
                        Log.i(TAG, "init remote [first time] Native Ads: ${native.id} - ${native.tag} - $preload")
                        existedIds.add(native.id!!)
                        CoreAds.instance.initAdapterNativeAdsMultiple(
                            activity.applicationContext,
                            activity,
                            native.id!!,
                            native.event ?: "DummyEventNative",
                            native.style ?: NativeStyle.BIG_13,
                            native.preload ?: 1,
                            preload == 0
                        )
                    }
                }
            }
        }
    }

    fun hasAllPermission(context: Context): Boolean {
        if (!Settings.canDrawOverlays(context)) return false

        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        appOpsManager ?: return false

        val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
