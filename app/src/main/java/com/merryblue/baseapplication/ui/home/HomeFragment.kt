package com.merryblue.baseapplication.ui.home

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.FragmentHomeBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.helpers.AppLoading
import com.merryblue.baseapplication.helpers.EDGE_REWARD_DAY
import com.merryblue.baseapplication.helpers.KEY_IS_CUSTOM
import com.merryblue.baseapplication.helpers.KEY_RECEIVE_DATA
import com.merryblue.baseapplication.helpers.PreviewType.EDGE_WALLPAPER_SCREEN
import com.merryblue.baseapplication.helpers.PreviewType.RIPPLE_WALLPAPER_SCREEN
import com.merryblue.baseapplication.helpers.PreviewType.STATIC_WALLPAPER_SCREEN
import com.merryblue.baseapplication.helpers.RIPPLE_PREMIUM
import com.merryblue.baseapplication.helpers.RIPPLE_RIPPLE
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_STOP
import com.merryblue.baseapplication.helpers.TYPE_PRESET
import com.merryblue.baseapplication.helpers.TYPE_THEME
import com.merryblue.baseapplication.helpers.WallpaperType
import com.merryblue.baseapplication.helpers.cache.WallpaperBgStore
import com.merryblue.baseapplication.helpers.getFullScreenTargetSize
import com.merryblue.baseapplication.helpers.openProperNetworkSettings
import com.merryblue.baseapplication.helpers.updateHeightForCurrentPage
import com.merryblue.baseapplication.helpers.video.VideoPreloader
import com.merryblue.baseapplication.service.edge.EdgeLightingOverlayService
import com.merryblue.baseapplication.ui.picker.ColorPickerActivity
import com.merryblue.baseapplication.ui.theme.ThemesActivity
import com.merryblue.baseapplication.ui.wallpaper.EdgePermissionViewModel
import com.merryblue.baseapplication.ui.wallpaper.EdgeWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.RippleWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.StaticWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.VideoWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.widget.BottomSheetEdgePermission
import com.merryblue.baseapplication.ui.widget.BottomSheetNoInternet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment
import kotlin.getValue

@AndroidEntryPoint
class HomeFragment: BaseFragment<FragmentHomeBinding>() {
    private val edgePermissionViewModel: EdgePermissionViewModel by activityViewModels()
    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var mediator: TabLayoutMediator
    private lateinit var presetAdapter: HomePresetAdapter
    private lateinit var homeThemeAdapter: HomeThemeAdapter
    private val prefs by lazy { AppPreferences(requireContext()) }
    private var currentType = WallpaperType.TYPE_STATIC
    private val presetOnClick: (Item) -> Unit = { handleItemClick(it) }
    private val customOnClick: () -> Unit = {
        startActivity(Intent(requireContext(), ColorPickerActivity::class.java))
    }

    private val overlayPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(requireContext())) {
            startEdgeOverlay()
        }
    }

    override fun getLayoutId() = R.layout.fragment_home

    override fun setUpViews() {
        initData()
        initTabLayout()
        initRecyclerView()
        registerOnClick()
    }

    private fun startEdgeOverlay() {
        if (!Settings.canDrawOverlays(requireContext())) {
            showBottomSheetEdgePermission()
            return
        }

        if (!EdgeLightingOverlayService.isRunning) {
            viewModel.updateEdgeState { it.copy(isEnableEdgeLighting = true) }
            ContextCompat.startForegroundService(requireContext(), Intent(requireContext(), EdgeLightingOverlayService::class.java))
        }
    }

    private fun showBottomSheetEdgePermission() {
        (parentFragmentManager.findFragmentByTag(BottomSheetEdgePermission.TAG) as? BottomSheetDialogFragment)?.dismissAllowingStateLoss()
        BottomSheetEdgePermission.newInstance().show(parentFragmentManager, BottomSheetEdgePermission.TAG)
    }

    private fun initData() {
        viewModel.loadPreset(EDGE_REWARD_DAY)

        val canSetLive = prefs.canChangeLive || prefs.canLiveChooser
        viewModel.loadThemes(if (canSetLive) RIPPLE_PREMIUM else RIPPLE_RIPPLE)
    }

    override fun setupObservers() {
        binding.apply {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                    launch {
                        edgePermissionViewModel.edgePermission.collect {
                            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${requireContext().packageName}".toUri())
                            overlayPermissionLauncher.launch(i)
                        }
                    }

                    launch {
                        viewModel.settingsEdgeLighting.collectLatest {
                            startEdgeOverlay()
                        }
                    }

                    launch {
                        viewModel.connectionState.collectLatest {
                            onNetworkStateChanged(it)
                            handleNoInternetBottomSheet(it)
                        }
                    }

                    launch {
                        viewModel.presetState.collectLatest { preset ->
                            presetAdapter.submitList(preset?.items?.take(3).orEmpty())
                        }
                    }

                    launch {
                        viewModel.themeState.collectLatest { theme ->
                            homeThemeAdapter.submitList(
                                buildList {
//                                    add(ThemeUi.Custom())     // todo: comment custom
                                    addAll(theme?.items?.take(3).orEmpty())
                                }
                            )
                        }
                    }

                    launch {
                        viewModel.bgBitmap.collectLatest { pair ->
                            AppLoading.closeLoading()

                            val key = pair.first
                            val bmp = pair.second
                            bmp?.let {

                                val canSetLive = prefs.canChangeLive || prefs.canLiveChooser

                                if (canSetLive) {
                                    when (key) {
                                        EDGE_WALLPAPER_SCREEN -> {
                                            WallpaperBgStore.saveFile(requireContext(), it)
                                            startActivity(Intent(requireContext(), EdgeWallpaperSettingsActivity::class.java))
                                        }

                                        RIPPLE_WALLPAPER_SCREEN -> {
                                            WallpaperBgStore.saveRippleAndNotify(requireContext(), it)
                                            startActivity(Intent(requireContext(), RippleWallpaperSettingsActivity::class.java))
                                        }

                                        STATIC_WALLPAPER_SCREEN -> {
                                            WallpaperBgStore.saveFile(requireContext(), it)
                                            startActivity(Intent(requireContext(), StaticWallpaperSettingsActivity::class.java))
                                        }
                                    }

                                } else {
                                    WallpaperBgStore.saveFile(requireContext(), it)
                                    startActivity(Intent(requireContext(), StaticWallpaperSettingsActivity::class.java))
                                }
                            } ?: run { Toast.makeText(requireContext(), getString(R.string.an_error_has_occurred), Toast.LENGTH_SHORT).show() }
                        }
                    }
                }
            }
        }
    }

    private fun handleNoInternetBottomSheet(isConnected: Boolean) {
        val fm = parentFragmentManager
        val current = fm.findFragmentByTag(BottomSheetNoInternet.TAG) as? BottomSheetDialogFragment

        if (isConnected) {
            if (current?.dialog?.isShowing == true) current.dismissAllowingStateLoss()
            return
        }

        if (current?.dialog?.isShowing == true) return

        BottomSheetNoInternet.newInstance {
            requireContext().openProperNetworkSettings()
        }.show(fm, BottomSheetNoInternet.TAG)
    }


    private fun initRecyclerView() {

        val presetManager = GridLayoutManager(requireContext(), 3)
        val themeManager = GridLayoutManager(requireContext(), 3)

        presetAdapter = HomePresetAdapter(presetOnClick)
        homeThemeAdapter = HomeThemeAdapter(customOnClick, presetOnClick)

        binding.apply {
            rcvHomePreset.apply {
                adapter = presetAdapter
                layoutManager = presetManager
                setHasFixedSize(true)
                itemAnimator = null
                isNestedScrollingEnabled = false
            }

            rcvHomeThemes.apply {
                adapter = homeThemeAdapter
                layoutManager = themeManager
                setHasFixedSize(true)
                itemAnimator = null
                isNestedScrollingEnabled = false
            }
        }
    }

    private fun registerOnClick() = with (binding) {

        btnViewAllTheme.setOnClickListener {
            val intent = Intent(requireContext(), ThemesActivity::class.java)
            intent.putExtra(KEY_IS_CUSTOM, true)
            intent.putExtra(KEY_RECEIVE_DATA, TYPE_THEME)
            startActivity(intent)
        }

        btnViewAllPreset.setOnClickListener {
            val intent = Intent(requireContext(), ThemesActivity::class.java)
            intent.putExtra(KEY_IS_CUSTOM, false)
            intent.putExtra(KEY_RECEIVE_DATA, TYPE_PRESET)
            startActivity(intent)
        }
    }

    private fun initTabLayout() {

        binding.vpSettingEdge.adapter = SettingEdgeAdapter(this@HomeFragment)

        val listSetting = buildList {
            add(getString(R.string.txt_color))
            add(getString(R.string.txt_effect))
            add(getString(R.string.txt_advanced))
        }
        mediator = TabLayoutMediator(binding.tabSettingEdge, binding.vpSettingEdge) { tab, p ->
            tab.text = listSetting[p]
        }
        mediator.attach()

        binding.vpSettingEdge.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.vpSettingEdge.post { binding.vpSettingEdge.updateHeightForCurrentPage() }
            }
        })
    }

    private fun handleItemClick(item: Item) {
        AppLoading.displayLoading(requireContext())

        currentType = item.type

        val canSetLive = prefs.canChangeLive || prefs.canLiveChooser

        if (canSetLive) {
            when (item.type) {
                WallpaperType.TYPE_EDGE -> viewModel.loadEdgeBackgroundUrl(item, requireContext().getFullScreenTargetSize())

                WallpaperType.TYPE_STATIC -> viewModel.loadStaticBackgroundUrl(item, requireContext().getFullScreenTargetSize())

                WallpaperType.TYPE_VIDEO -> {
                    viewModel.videoUrl = item.pathUrl
                    VideoPreloader.preload(requireContext().applicationContext, item.pathUrl) {
                        AppLoading.closeLoading()
                    }
                    viewModel.updateEdgeState { it.copy(isEnableEdgeLighting = false) }
                    viewModel.sendActionBroadcast(ACTION_EDGE_OVERLAY_STOP)
                    startActivity(Intent(requireContext(), VideoWallpaperSettingsActivity::class.java))
                }

                WallpaperType.TYPE_RIPPLE -> {
                    viewModel.rippleEffectUrl = item.pathUrl
                    viewModel.loadBackgroundRippleUrl(item, requireContext().getFullScreenTargetSize())
                }
            }

        } else viewModel.loadStaticBackgroundUrl(item, requireContext().getFullScreenTargetSize())

    }

    fun onChildContentExpanded() = binding.apply {
        vpSettingEdge.post { vpSettingEdge.updateHeightForCurrentPage() }
        nestedScrollHome.post { nestedScrollHome.requestLayout() }
    }

    override fun onDestroyView() {
        if (::mediator.isInitialized) mediator.detach()
        super.onDestroyView()
    }
}
