package com.merryblue.baseapplication.ui.theme

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityThemesBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.enums.InterstitialFunction
import com.merryblue.baseapplication.helpers.AppLoading
import com.merryblue.baseapplication.helpers.KEY_IS_CUSTOM
import com.merryblue.baseapplication.helpers.KEY_RECEIVE_DATA
import com.merryblue.baseapplication.helpers.RIPPLE_MAGICAL_BORDERS
import com.merryblue.baseapplication.helpers.RIPPLE_RIPPLE
import com.merryblue.baseapplication.helpers.RIPPLE_TOP_PICS
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_STOP
import com.merryblue.baseapplication.helpers.TYPE_PRESET
import com.merryblue.baseapplication.helpers.WallpaperType
import com.merryblue.baseapplication.helpers.cache.WallpaperBgStore
import com.merryblue.baseapplication.helpers.getFullScreenTargetSize
import com.merryblue.baseapplication.helpers.openProperNetworkSettings
import com.merryblue.baseapplication.helpers.video.VideoPreloader
import com.merryblue.baseapplication.ui.wallpaper.EdgeWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.ParallaxWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.RippleWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.StaticWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.VideoWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.widget.BottomSheetNoInternet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.showMessage

@AndroidEntryPoint
class ThemesActivity : BaseActivity<ActivityThemesBinding>() {
    private val viewModel: ThemeViewModel by viewModels()

    override fun getLayoutId(): Int = R.layout.activity_themes

    private var initType: String? = TYPE_PRESET
    private var isCustom: Boolean = false
    private var currentType: Item? = null

    private lateinit var mediator: TabLayoutMediator

    override fun setUpViews() {
        hideNavigationBar(binding.main)

        initType = intent.getStringExtra(KEY_RECEIVE_DATA)
        isCustom = intent.getBooleanExtra(KEY_IS_CUSTOM, false)
        initTabLayout()
        super.setUpViews()
    }

    override fun setUpObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.connectionState.collectLatest {
                        onNetworkStateChanged(it)
                        handleNoInternetBottomSheet(it)
                    }
                }
            }
        }
    }

    override fun onCloseAction() {
        handleAdsCompleted()
    }

    fun handleItemClick(item: Item) {
        currentType = item
        showInterstitialBy(InterstitialFunction.ViewTheme.name) {
            AppLoading.displayLoading(this)
            handleAdsCompleted()
        }
    }

    private fun handleAdsCompleted() {
        val item = currentType ?: return
        val canSetLive = viewModel.canSetLive()
        if (canSetLive) {
            when (item.type) {
                WallpaperType.TYPE_EDGE -> {
                    viewModel.loadEdgeBackgroundUrl(item, getFullScreenTargetSize()) { bm ->
                        AppLoading.closeLoading()
                        bm?.let {
                            WallpaperBgStore.saveFile(this, it)
                            startActivity(Intent(this, EdgeWallpaperSettingsActivity::class.java))
                        } ?: run {
                            showMessage(getString(R.string.an_error_has_occurred))
                        }
                    }
                }
                WallpaperType.TYPE_STATIC -> {
                    viewModel.loadStaticBackgroundUrl(item, getFullScreenTargetSize()) { bm ->
                        AppLoading.closeLoading()
                        bm?.let {
                            WallpaperBgStore.saveFile(this, it)
                            startActivity(Intent(this, StaticWallpaperSettingsActivity::class.java))
                        } ?: run {
                            showMessage(getString(R.string.an_error_has_occurred))
                        }
                    }
                }

                WallpaperType.TYPE_VIDEO -> {
                    viewModel.videoUrl = item.pathUrl
                    VideoPreloader.preload(this, item.pathUrl) {
                        AppLoading.closeLoading()
                    }
                    viewModel.updateEdgeState { it.copy(isEnableEdgeLighting = false) }
                    viewModel.sendActionBroadcast(ACTION_EDGE_OVERLAY_STOP)
                    startActivity(Intent(this, VideoWallpaperSettingsActivity::class.java))
                }

                WallpaperType.TYPE_RIPPLE -> {
                    AppLoading.closeLoading()
                    viewModel.rippleEffectUrl = item.pathUrl
                    viewModel.loadBackgroundRippleUrl(item, getFullScreenTargetSize()) { bm ->
                        bm?.let {
                            WallpaperBgStore.saveRippleAndNotify(this, it)
                            startActivity(Intent(this, RippleWallpaperSettingsActivity::class.java))
                        } ?: run {
                            showMessage(getString(R.string.an_error_has_occurred))
                        }
                    }
                }

                WallpaperType.TYPE_PARALLAX -> {
                    AppLoading.closeLoading()
                    viewModel.updateEdgeState { it.copy(isEnableEdgeLighting = false) }
                    viewModel.sendActionBroadcast(ACTION_EDGE_OVERLAY_STOP)
                    startActivity(
                        Intent(this, ParallaxWallpaperSettingsActivity::class.java)
                            .putExtra(ParallaxWallpaperSettingsActivity.EXTRA_PENDING_BACKGROUND_URL, item.pathUrl)
                    )
                }
            }
        } else {
            viewModel.loadStaticBackgroundUrl(item, getFullScreenTargetSize()) { bm ->
                AppLoading.closeLoading()
                bm?.let {
                    WallpaperBgStore.saveFile(this, it)
                    startActivity(Intent(this, StaticWallpaperSettingsActivity::class.java))
                } ?: run {
                    showMessage(getString(R.string.an_error_has_occurred))
                }
            }
        }
        currentType = null
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

    private fun initTabLayout() = binding.apply {
        tvTitleTheme.text = getString(if (initType == TYPE_PRESET) R.string.txt_preset else R.string.txt_themes)

        val canSetLive = viewModel.canSetLive()

        val titles = when (initType) {
//            TYPE_PRESET -> listOf(
//                EDGE_REWARD_DAY to getString(R.string.txt_daily_rewards),
//                EDGE_TRENDING to getString(R.string.txt_trending_today),
//                EDGE_MOST to getString(R.string.txt_most_downloaded),
//                EDGE_FIM to getString(R.string.txt_static)
//            )

            TYPE_PRESET -> listOf(RIPPLE_TOP_PICS to getString(R.string.txt_top_pics))
            else -> buildList {
                if (canSetLive) add(RIPPLE_MAGICAL_BORDERS to getString(R.string.txt_magical_borders))
                else add(RIPPLE_RIPPLE to getString(R.string.txt_ripple))
            }
        }

        vpThemes.adapter = ThemePagerAdapter(isCustom, this@ThemesActivity, titles)
        mediator = TabLayoutMediator(tabTheme, vpThemes) { tab, position ->
            tab.text = titles[position].second
        }.apply { attach() }
        vpThemes.offscreenPageLimit = 3
        vpThemes.isUserInputEnabled = false
        tabTheme.visibility = View.GONE

        btnBackTheme.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediator.isInitialized) mediator.detach()
    }

    class ThemePagerAdapter(isCustom: Boolean, activity: FragmentActivity, titles: List<Pair<String, String>>): FragmentStateAdapter(activity) {

        private val fragments by lazy {
            titles.mapIndexed { index, it ->
                ThemeChildFragment.newInstance(
                    it.first,
                    false,
                    if (isCustom) index == 0 else false
                )
            }
        }

        override fun getItemCount() = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}