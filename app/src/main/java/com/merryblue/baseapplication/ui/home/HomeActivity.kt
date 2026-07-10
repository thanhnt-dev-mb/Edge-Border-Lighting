package com.merryblue.baseapplication.ui.home

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.NavigationRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.BuildConfig
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityHomeBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.enums.InterstitialFunction
import com.merryblue.baseapplication.helpers.Compatibility
import com.merryblue.baseapplication.helpers.canHandleIntent
import com.merryblue.baseapplication.helpers.openProperNetworkSettings
import com.merryblue.baseapplication.ui.appupdate.ForceUpdateActivity
import com.merryblue.baseapplication.ui.iap.PurchaseActivity
import com.merryblue.baseapplication.ui.setting.SettingFragment
import com.merryblue.baseapplication.ui.view.EdgeBottomNavView
import com.merryblue.baseapplication.ui.widget.BottomSheetNoInternet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.ads.remoteconfig.CoreRemoteConfig
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.hide
import org.app.core.base.extensions.openActivityAndClearStack
import org.app.core.base.utils.StringResId
import kotlin.getValue


@AndroidEntryPoint
class HomeActivity : BaseActivity<ActivityHomeBinding>() {
    private var isFirstVisible = true
    private var isActive: Boolean = false
    private lateinit var navControllers: Map<EdgeBottomNavView.Tab, NavController>
    private var currentTab: EdgeBottomNavView.Tab = EdgeBottomNavView.Tab.EDGE
    private val prefs by lazy { AppPreferences(this) }
    private val homeViewModel: HomeViewModel by viewModels()


    override
    fun getLayoutId() = R.layout.activity_home

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        requestPostNotificationPermissionIfNeed()
    }

    override fun setupBinding() {
        nativeFullContainer = binding.nativeFullContainer
        closeNativeFullAds = binding.closeNativeFullAds
        binding.nativeFullContainer.hide()
        binding.closeNativeFullAds.hide()
    }

    override fun onResume() {
        super.onResume()

        isFirstVisible = false
        isActive = true
        val ret = handleForceUpdateIfNeed()
        if (!ret) {
            if (homeViewModel.showIAP()) {
                PurchaseActivity.open(this, "home")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isActive = false
    }

    override fun setUpViews() {
        hideNavigationBar(binding.main)
        binding.bottomNav.setSelectedTab(EdgeBottomNavView.Tab.EDGE)
        binding.bottomNav.setOnTabSelectedListener { showTab(it) }
        initDeviceSupport()
        setupBottomNavMultiStack()
        showBottomBanner(binding.layoutCard, binding.adsContainer)
        super.setUpViews()
    }

    private fun initDeviceSupport() {
        prefs.canChangeLive = canHandleIntent(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER))
        prefs.canLiveChooser = canHandleIntent(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
    }

    override fun setUpObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeViewModel.connectionState.collectLatest {
                        onNetworkStateChanged(it)
                        handleNoInternetBottomSheet(it)
                    }
                }
            }
        }
    }

    override fun onCloseAction() {
        homeViewModel.sendAdsCompleteEvent()
    }

    fun showInterstitial() {
        showInterstitialBy(InterstitialFunction.ViewTheme.name) {
            homeViewModel.sendAdsCompleteEvent()
        }
    }

    private fun handleNoInternetBottomSheet(isConnected: Boolean) {
        val fm = supportFragmentManager
        val current = fm.findFragmentByTag(BottomSheetNoInternet.TAG) as? BottomSheetDialogFragment

        if (isConnected) {
            if (current?.dialog?.isShowing == true) current.dismissAllowingStateLoss()
            return
        }

        if (current?.dialog?.isShowing == true) return

        BottomSheetNoInternet.newInstance {
            this.openProperNetworkSettings()
        }.show(fm, BottomSheetNoInternet.TAG)
    }

    private fun setupBottomNavMultiStack() {
        val fm = supportFragmentManager

        fun createHost(@NavigationRes graphId: Int, tag: String): NavHostFragment {
            val host = NavHostFragment.create(graphId)
            fm.beginTransaction()
                .add(R.id.navHostContainerHome, host, tag)
                .hide(host)
                .commitNow()
            return host
        }

        val edgeHost = createHost(R.navigation.nav_home, "edge")
        val wallHost = createHost(R.navigation.nav_wallpaper, "wallpaper")
        val setHost  = createHost(R.navigation.nav_setting, "setting")

        navControllers = mapOf(
            EdgeBottomNavView.Tab.EDGE to edgeHost.navController,
            EdgeBottomNavView.Tab.WALLPAPER to wallHost.navController,
            EdgeBottomNavView.Tab.SETTING to setHost.navController
        )

        showTab(EdgeBottomNavView.Tab.EDGE)
    }

    private fun showTab(tab: EdgeBottomNavView.Tab) {
        val fm = supportFragmentManager
        val tx = fm.beginTransaction()

        val edge = fm.findFragmentByTag("edge")!!
        val wall = fm.findFragmentByTag("wallpaper")!!
        val set  = fm.findFragmentByTag("setting")!!

        tx.hide(edge).hide(wall).hide(set)

        val toShow = when (tab) {
            EdgeBottomNavView.Tab.EDGE -> edge
            EdgeBottomNavView.Tab.WALLPAPER -> wall
            EdgeBottomNavView.Tab.SETTING -> set
        }

        tx.show(toShow).commitNow()
        currentTab = tab

        checkTabSetting(tab, set)
    }

    private fun checkTabSetting(tab: EdgeBottomNavView.Tab, set: Fragment, ) {
        if (tab == EdgeBottomNavView.Tab.SETTING) {
            val current = set.childFragmentManager.primaryNavigationFragment
            if (current is SettingFragment) {
                current.updateEdgeToggle()
            } else {
                val nested = (current as? NavHostFragment)?.childFragmentManager?.primaryNavigationFragment
                (nested as? SettingFragment)?.updateEdgeToggle()
            }
        }
    }


    private fun handleForceUpdateIfNeed() : Boolean {
        val isForceUpdate = CoreRemoteConfig.instance.checkForceUpdateIfNeed(BuildConfig.VERSION_CODE)
        if (isForceUpdate) {
            Handler(Looper.getMainLooper()).postDelayed({
                openActivityAndClearStack(ForceUpdateActivity::class.java)
            }, 300)
        }
        return isForceUpdate
    }

    private fun requestPostNotificationPermissionIfNeed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (!Compatibility.hasPostNotificationsPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
            requestPermissions(arrayOf(
                Manifest.permission.POST_NOTIFICATIONS
            )) { isGranted: Boolean ->
                if (!isGranted) {
                    Toast.makeText(this, getString(StringResId.notificationOff), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        val nav = navControllers[currentTab] ?: return super.onBackPressed()
        if (!nav.popBackStack()) {
            if (currentTab != EdgeBottomNavView.Tab.EDGE) {
                showTab(EdgeBottomNavView.Tab.EDGE)
                binding.bottomNav.setSelectedTab(EdgeBottomNavView.Tab.EDGE)
            } else {
                super.onBackPressed()
            }
        }
    }
}
