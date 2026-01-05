package com.merryblue.baseapplication

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.helpers.AdsUnit
import com.merryblue.baseapplication.helpers.AppExceptionHandler
import com.merryblue.baseapplication.ui.splash.SplashActivity
import dagger.hilt.android.HiltAndroidApp
import org.app.core.ads.CoreAds
import org.app.core.ads.TEST_DEVICE_IDS
import org.app.core.ads.openads.AdapterOpenAppManager
import org.app.core.ads.remoteconfig.CoreRemoteConfig
import org.app.core.base.BaseApplication
import org.app.core.base.utils.TimberDebugTree
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class Application : BaseApplication(),
    Application.ActivityLifecycleCallbacks,
    LifecycleObserver,
    Configuration.Provider,
    AdapterOpenAppManager.Callback {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appRepository: AppRepository

    private var currentActivity: Activity? = null

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree())
        }
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        try {
            Thread.setDefaultUncaughtExceptionHandler(AppExceptionHandler(this))
        } catch (_: Exception) {
        }

        CoreAds.instance.initAdsAdapter(
            context = this,
            listTestDeviceId = TEST_DEVICE_IDS,
            ctaBackgroundResource = R.drawable.bg_ads_cta_bg
        ) { }

        val openAppId = CoreRemoteConfig.instance.findAppOpenId() ?: ""
        AdapterOpenAppManager.instance.registerLifecycle(this, openAppId, this)
        AdapterOpenAppManager.instance.registerDisableOpenAdsAt(SplashActivity::class.java)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        Timber.i("DEBUG---> onMoveToForeground: ")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppStopped() {
        Timber.i("DEBUG---> onAppStopped")
        appRepository.setShowLock()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun getOpenAdsId(): String = AdsUnit.SwitchApp.id

    override fun initAds(activity: Activity?) {

    }

    override fun onAdFullScreenCompleted(activity: Activity?) {

    }

    override fun onAdFullScreenShow(activity: Activity?) {

    }

    override fun getListTestDeviceId(): List<String> {
        return if (BuildConfig.DEBUG) {
            TEST_DEVICE_IDS
        } else emptyList()
    }
}
