package com.merryblue.baseapplication.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.NavigationRes
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.BuildConfig
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.edge.DisplayHole
import com.merryblue.baseapplication.coredata.model.edge.DisplayInfinity
import com.merryblue.baseapplication.coredata.model.edge.DisplayNotch
import com.merryblue.baseapplication.coredata.model.edge.DisplayNotchType
import com.merryblue.baseapplication.coredata.model.edge.EdgeSelection
import com.merryblue.baseapplication.coredata.model.edge.EdgeSettings
import com.merryblue.baseapplication.databinding.ActivityHomeBinding
import com.merryblue.baseapplication.helpers.Compatibility
import com.merryblue.baseapplication.helpers.isAppInstalled
import com.merryblue.baseapplication.helpers.isBackground
import com.merryblue.baseapplication.helpers.openPolicy
import com.merryblue.baseapplication.ui.appupdate.ForceUpdateActivity
import com.merryblue.baseapplication.ui.onboard.language.LanguageActivity
import com.merryblue.baseapplication.ui.view.EdgeBottomNavView
import com.merryblue.baseapplication.ui.widget.BottomSheetRate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.app.core.ads.remoteconfig.CoreRemoteConfig
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.openActivityAndClearStack
import org.app.core.base.utils.StringResId


@AndroidEntryPoint
class HomeActivity : BaseActivity<ActivityHomeBinding>() {
    private val viewModel: HomeViewModel by viewModels()

    private var isFirstVisible = true
    private var showingRate: Boolean = false
    private var isActive: Boolean = false
    private lateinit var navControllers: Map<EdgeBottomNavView.Tab, NavController>
    private var currentTab: EdgeBottomNavView.Tab = EdgeBottomNavView.Tab.EDGE

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
        setupBottomNavMultiStack()
    }

    override fun setUpObserver() {
        binding.apply {
            lifecycleScope.launch {

                launch { viewModel.edgeVisibilityEvents.collect { edgeView.visibility = if (it) View.VISIBLE else View.INVISIBLE } }

                launch {
                    viewModel.edgeColorEvents.collect { edgeSelection ->
                        when (edgeSelection) {
                            is EdgeSelection.EdgeColor -> edgeView.applyPreset(edgeSelection.preset)
                            is EdgeSelection.EdgeEffect -> if (edgeSelection.selectedIndex == 0) edgeView.setPatternEnabled(false) else edgeView.applyPreset(edgeSelection.preset)
                            is EdgeSelection.EdgeAdvanced -> {
                                // todo: do something
                            }
                        }
                    }
                }

                launch {
                    viewModel.edgeSettingsEvents.collect { edgeSettings ->
                        when (edgeSettings) {
                            is EdgeSettings.EdgeSpeed -> edgeView.setSpeedMs(edgeSettings.progress)
                            is EdgeSettings.EdgeSize -> edgeView.setSizePx(edgeSettings.progress)
                            is EdgeSettings.EdgeBottomRadius -> edgeView.setBottomRadiusPx(edgeSettings.progress)
                            is EdgeSettings.EdgeTopRadius -> edgeView.setTopRadiusPx(edgeSettings.progress)
                        }
                    }
                }

                launch { viewModel.edgeAdvancedEvents.collect { edgeView.setAdvanced(it) } }
                launch { viewModel.edgeHoleTypeEvents.collect { edgeView.setHoleShape(it) } }
                launch { viewModel.edgeInfinityTypeEvents.collect { edgeView.setInfinityShape(it, false) } }

                launch {
                    viewModel.edgeDisplayNotchTypeEvents.collect { displayNotchType ->
                        val progress = displayNotchType.progress

                        when (displayNotchType) {
                            is DisplayNotchType.TypeDisplayNotch -> {
                                when (displayNotchType.type) {
                                    DisplayNotch.NOTCH_WIDTH -> edgeView.setNotchWidthFraction(progress)
                                    DisplayNotch.NOTCH_HEIGHT -> edgeView.setNotchHeightPx(progress)
                                    DisplayNotch.NOTCH_TOP_RADIUS -> edgeView.setNotchTopRadiusPx(progress)
                                    DisplayNotch.NOTCH_BOTTOM_RADIUS -> edgeView.setNotchBottomRadiusPx(progress)
                                    DisplayNotch.NOTCH_BOTTOM_FULLNESS -> edgeView.setNotchBottomFullness(progress)
                                }
                            }

                            is DisplayNotchType.TypeDisplayHole -> {
                                when (displayNotchType.type) {
                                    DisplayHole.HOLE_HORIZONTAL -> edgeView.setHoleOffsetXProgress(progress)
                                    DisplayHole.HOLE_VERTICAL -> edgeView.setHoleOffsetYProgress(progress)
                                    DisplayHole.HOLE_RADIUS -> edgeView.setHoleCircleRadiusPx(progress)
                                    DisplayHole.HOLE_WIDTH -> edgeView.setHoleRoundWidthPx(progress)
                                    DisplayHole.HOLE_HEIGHT -> edgeView.setHoleRoundHeightPx(progress)
                                    DisplayHole.HOLE_CORNER -> edgeView.setHoleRoundCornerRadiusPx(progress)
                                }
                            }

                            is DisplayNotchType.TypeDisplayInfinity -> {
                                when (displayNotchType.type) {
                                    DisplayInfinity.INFINITY_TOP -> edgeView.setInfinityRadiusTopPx(progress)
                                    DisplayInfinity.INFINITY_WIDTH -> edgeView.setInfinityWidthPx(progress)
                                    DisplayInfinity.INFINITY_HEIGHT -> edgeView.setInfinityHeightPx(progress)
                                }
                            }
                        }
                    }
                }
            }
        }
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

        tx.show(toShow).commit()
        currentTab = tab
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
                    startActivity(intent);
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
