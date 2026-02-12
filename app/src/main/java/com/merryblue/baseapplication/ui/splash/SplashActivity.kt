package com.merryblue.baseapplication.ui.splash

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivitySplashBinding
import com.merryblue.baseapplication.service.AlarmService
import com.merryblue.baseapplication.ui.home.HomeActivity
import com.merryblue.baseapplication.ui.onboard.language.LanguageActivity
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.ads.CoreAds
import org.app.core.ads.GoogleMobileAdsConsentManager
import org.app.core.ads.base.NativeStyle
import org.app.core.ads.callback.AdsCallback
import org.app.core.ads.callback.LoadCallback
import org.app.core.ads.openads.AdapterOpenAppManager
import org.app.core.ads.remoteconfig.CoreRemoteConfig
import org.app.core.ads.remoteconfig.config.AdsConfigure
import org.app.core.ads.remoteconfig.config.Interstitial
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.hide
import org.app.core.base.extensions.invisible
import org.app.core.base.extensions.openActivityAndClearStack
import org.app.core.base.extensions.show
import org.app.core.base.utils.StringResId
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    private val CONST_ADS_INITIALIZED = 1 // 0x01
    private val CONST_REMOTE_CONFIG_INITIALIZED = 2 // 0x10
    private val CONST_ALL_INITIALIZED = 3 // 0x11
    
    private val viewModel: SplashViewModel by viewModels()
    private val isInitializeCalled = AtomicBoolean(false)
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private var _status: Int = 0
    private var _isGoHome = false

    override
    fun getLayoutId() = R.layout.activity_splash

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
                Timber.i("FCMService -> FCM Token: ${task.result}")
            }
        )

        val alarmService = AlarmService(this)
        alarmService.configureAlarmAndWorker()
    }

    override fun setupBinding() {

    }

    @SuppressLint("LogNotTimber")
    override
    fun setUpViews() {
        supportActionBar?.hide()

        _status = _status.or(CONST_ADS_INITIALIZED)
        val isHideAds = viewModel.isPremium()
        initRemoteConfig(isHideAds)
        if (isHideAds) {
            binding.layoutCard.invisible()
            CoreAds.instance.setHideAds(true)
            AdapterOpenAppManager.instance.disableOpenAds()
            if (viewModel.isFirstTime()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    LanguageActivity.open(this)
                }, 1500)
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    openHome()
                }, 2000)
            }
            return
        }

        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
        googleMobileAdsConsentManager.gatherConsent(this, false) { consentError ->
            if (consentError != null) {
                // Consent not obtained in current session.
                Log.i(TAG, "${consentError.errorCode}. ${consentError.message}")
            }

            val flag = googleMobileAdsConsentManager.canRequestAds
            Log.i( "#####MB_TAG", "can request ads flag: $flag")
            if (flag || !viewModel.connectionState.value) {
                if (_status == CONST_ALL_INITIALIZED) {
                    initializeAds()
                }
            } else {
                CoreAds.instance.setHideAds(true)
                binding.layoutCard.invisible()
                if (viewModel.isFirstTime()) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        LanguageActivity.open(this)
                    }, 1500)
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        openHome()
                    }, 1500)
                }
            }
        }
        
        if (googleMobileAdsConsentManager.canRequestAds) {
            if (_status == CONST_ALL_INITIALIZED) {
                initializeAds()
            }
        }
    }
    
    private fun initializeAds() {
        if (isInitializeCalled.getAndSet(true)) {
            return
        }
        val openAppId = CoreRemoteConfig.instance.findAppOpenId() ?: ""
        AdapterOpenAppManager.instance.setOpenAdsId(openAppId)
        AdapterOpenAppManager.instance.preloadAds()
        val remoteConfig = viewModel.getRemoteConfiguration()
        if (remoteConfig != null) {
            Timber.i("Start loading ads...")
            val isFirstTime = viewModel.isFirstTime()
            val hasSplashNative = showAds(remoteConfig, isFirstTime)

            CoreAds.instance.logFirebaseEvent(if (isFirstTime) "AppStartSessionFirstTime" else "AppStartSessionNextTime")
            _isGoHome = false
            showStartApp(remoteConfig, if (hasSplashNative) 3000 else 1000)
        } else {
            CoreAds.instance.setHideAds(true)
            binding.layoutCard.invisible()
            if (viewModel.isFirstTime()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    openActivityAndClearStack(LanguageActivity::class.java)
                }, 1500)
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    openHome()
                }, 1500)
            }
        }
    }
    
    private fun openHome() {
        if (_isGoHome) return

        try {
            _isGoHome = true
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
            val purposesConsents = sharedPref.getString("IABTCF_PurposeConsents", "") ?: ""
            Timber.i(TAG, "onCreate: purposesConsents -> $purposesConsents")
            if (purposesConsents.isNotEmpty()) {
                val purposeOneString = purposesConsents.first()
                Timber.i(TAG, "onCreate: purposeOneString -> $purposeOneString")
                if (purposeOneString != '1') {
                    CoreAds.instance.userConsent = false
                    CoreAds.instance.logFirebaseEvent("UserNotConsent")
                } else {
                    CoreAds.instance.userConsent = true
                    CoreAds.instance.logFirebaseEvent("UserHadConsent")
                }
            } else {
                CoreAds.instance.logFirebaseEvent("UserConsentOk")
            }

            if (viewModel.isFirstTime()) {
                openActivityAndClearStack(LanguageActivity::class.java)
            } else {
                openActivityAndClearStack(HomeActivity::class.java)
            }
        } catch (_: Exception) {}
    }
    
    private fun initRemoteConfig(hidesAds: Boolean = false) {
        CoreRemoteConfig.instance.init(this, true, object : LoadCallback() {
            override fun onLoadSuccess() {
                if (!hidesAds) {
                    _status = _status.or(CONST_REMOTE_CONFIG_INITIALIZED)
                    if (_status == CONST_ALL_INITIALIZED) {
                        initializeAds()
                    }
                }
            }
            
            @SuppressLint("LogNotTimber")
            override fun onLoadFailed(message: String?) {
                _status = _status.or(CONST_REMOTE_CONFIG_INITIALIZED)
                if (!hidesAds) {
                    Log.i(TAG, "Error: $message")
                    CoreRemoteConfig.instance.setLocalConfig(viewModel.getJsonConfiguration(), true)
                    if (_status == CONST_ALL_INITIALIZED) {
                        initializeAds()
                    }
                }
            }
        })
    }

    private fun showAds(remoteConfig: AdsConfigure, firstLaunch: Boolean) : Boolean {
        val tagNative = "SplashActivity_Native"
        val nativeAds = remoteConfig.natives?.firstOrNull {
            it.tag == tagNative && !it.id.isNullOrBlank()
        }
        val preload = if (firstLaunch) (nativeAds?.preload ?: 0) else 1

        if (nativeAds != null) {
            binding.layoutCard.show()
            CoreAds.instance.initAdapterNativeAdsMultiple(
                applicationContext,
                this,
                nativeAds.id!!,
                nativeAds.event ?: "ClickSplashDummy",
                nativeAds.style ?: NativeStyle.BIG_13,
                preload,
                false,
                binding.adsContainer
            )

            return true
        } else {
            binding.layoutCard.invisible()
            viewModel.preloadNative(this)
            return false
        }
    }

    private fun showStartApp(remoteConfig: AdsConfigure, delay: Long = 2000) {
        val startAppAds = remoteConfig.splash?.firstOrNull()

        if (startAppAds == null || startAppAds.id.isNullOrBlank()) {
            viewModel.preloadBanner(this)
            openHome()
        } else {
            viewModel.preloadBanner(this)
            if (viewModel.isFirstTime()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    showLaunchingOpenApp()
                }, delay)
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    showLaunchingInterstitial(startAppAds)
                }, delay)
            }
        }
    }

    private fun showLaunchingOpenApp() {
        AdapterOpenAppManager.instance.showLaunchingOpenApp {
            openHome()
        }
    }

    private fun showLaunchingInterstitial(ads: Interstitial) {
        CoreAds.instance.showAdapterInterstitialSplashAds(
            getString(StringResId.loadingAds),
            this,
            ads.id!!,
            ads.event ?: "ClickStartAppDummy",
            15000,
            object : AdsCallback() {
                override fun onClosed() {
                    super.onClosed()
                    openHome()
                }

                override fun onError(message: String?) {
                    super.onError(message)
                    openHome()
                }
            })
    }
}
