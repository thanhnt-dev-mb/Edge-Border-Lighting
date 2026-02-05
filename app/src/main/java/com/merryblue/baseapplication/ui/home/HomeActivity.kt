package com.merryblue.baseapplication.ui.home

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.NavigationRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.BuildConfig
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityHomeBinding
import com.merryblue.baseapplication.helpers.Compatibility
import com.merryblue.baseapplication.helpers.canHandleIntent
import com.merryblue.baseapplication.helpers.isAppInstalled
import com.merryblue.baseapplication.helpers.isBackground
import com.merryblue.baseapplication.helpers.openPolicy
import com.merryblue.baseapplication.ui.appupdate.ForceUpdateActivity
import com.merryblue.baseapplication.ui.onboard.language.LanguageActivity
import com.merryblue.baseapplication.ui.setting.SettingFragment
import com.merryblue.baseapplication.ui.view.EdgeBottomNavView
import com.merryblue.baseapplication.ui.widget.BottomSheetRate
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.ads.remoteconfig.CoreRemoteConfig
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.openActivityAndClearStack
import org.app.core.base.utils.StringResId


@AndroidEntryPoint
class HomeActivity : BaseActivity<ActivityHomeBinding>() {
    private var isFirstVisible = true
    private var showingRate: Boolean = false
    private var isActive: Boolean = false
    private lateinit var navControllers: Map<EdgeBottomNavView.Tab, NavController>
    private var currentTab: EdgeBottomNavView.Tab = EdgeBottomNavView.Tab.EDGE
    private val prefs by lazy { AppPreferences(this) }

    override
    fun getLayoutId() = R.layout.activity_home

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
        requestPostNotificationPermissionIfNeed()
    }

    override fun onResume() {
        super.onResume()

//        handleForceUpdateIfNeed()
        isFirstVisible = false
        isActive = true
    }

    override fun onPause() {
        super.onPause()
        isActive = false
    }

    override fun setUpViews() {
        binding.bottomNav.setSelectedTab(EdgeBottomNavView.Tab.EDGE)
        binding.bottomNav.setOnTabSelectedListener { showTab(it) }
        initDeviceSupport()
        setupBottomNavMultiStack()
    }

    private fun initDeviceSupport() {
        prefs.canChangeLive = canHandleIntent(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER))
        prefs.canLiveChooser = canHandleIntent(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
    }

    override fun setUpObserver() = Unit

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

    fun handleShowReviewIfNeed() {
        if (!isActive || isBackground() || showingRate) return
    
        try {
            showingRate = true
            (supportFragmentManager.findFragmentByTag(BottomSheetRate.TAG) as? BottomSheetDialogFragment)?.dismissAllowingStateLoss()
            val bottom = BottomSheetRate {

            }
            bottom.show(supportFragmentManager, BottomSheetRate.TAG)
        } catch (ex: Exception) {
            showingRate = false
            ex.printStackTrace()
        }
    }

    private fun handleDrawerMenuAction(itemId: Int) {
        when(itemId) {
            R.id.language -> {
                LanguageActivity.open(this, "setting")
            }
            R.id.rate -> {
                handleShowReviewIfNeed()
            }
            R.id.feedback -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback.developer.app@gmail.com"))
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                val gmailPkg = "com.google.android.gm"
                val isGmailInstalled = isAppInstalled(gmailPkg)
                if (isGmailInstalled) {
                    intent.type = "text/html"
                    intent.setPackage(gmailPkg)
                    startActivity(intent)
                } else {
                    intent.type = "message/rfc822"
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        startActivity(Intent.createChooser(intent, "Choose an Email application to start"))
                    }
                }
                return
            }
            R.id.privacy -> {
                openPolicy()
            }
            else -> {}
        }
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
