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
import com.google.android.material.tabs.TabLayoutMediator
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.FragmentHomeBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.ThemeUi
import com.merryblue.baseapplication.helpers.EDGE_REWARD_DAY
import com.merryblue.baseapplication.helpers.KEY_IS_CUSTOM
import com.merryblue.baseapplication.helpers.KEY_RECEIVE_DATA
import com.merryblue.baseapplication.helpers.PreviewType.KEY_EDGE
import com.merryblue.baseapplication.helpers.PreviewType.KEY_RIPPLE
import com.merryblue.baseapplication.helpers.RIPPLE_PREMIUM
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_WALLPAPER_STATE_STOP
import com.merryblue.baseapplication.helpers.TYPE_PRESET
import com.merryblue.baseapplication.helpers.TYPE_THEME
import com.merryblue.baseapplication.helpers.WallpaperType
import com.merryblue.baseapplication.helpers.cache.WallpaperBgStore
import com.merryblue.baseapplication.helpers.getFullScreenTargetSize
import com.merryblue.baseapplication.helpers.updateHeightForCurrentPage
import com.merryblue.baseapplication.helpers.video.VideoPreloader
import com.merryblue.baseapplication.service.EdgeLightingOverlayService
import com.merryblue.baseapplication.ui.picker.ColorPickerActivity
import com.merryblue.baseapplication.ui.theme.ThemesActivity
import com.merryblue.baseapplication.ui.wallpaper.EdgeWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.RippleWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.VideoWallpaperSettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var mediator: TabLayoutMediator
    private lateinit var presetAdapter: HomePresetAdapter
    private lateinit var homeThemeAdapter: HomeThemeAdapter
    private val presetOnClick: (Item) -> Unit = { handleItemClick(it) }
    private val customOnClick: () -> Unit = {
        disableEdgeLighting()
        startActivity(Intent(requireContext(), ColorPickerActivity::class.java))
    }
    private val overlayPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(requireContext())) {
            startEdgeOverlay()
        } else {
            binding.edgeToggle.isChecked = false
            viewModel.isToggleEdgeFirstTime = false
        }
    }

    override fun getLayoutId() = R.layout.fragment_home

    override fun onResume() {
        super.onResume()
        if (viewModel.isToggleEdgeFirstTime) viewModel.restartOverlay()
    }

    override fun setUpViews() {
        viewModel.loadPreset(EDGE_REWARD_DAY)
        viewModel.loadThemes(RIPPLE_PREMIUM)

        initTabLayout()
        initRecyclerView()
        registerOnClick()
    }

    override fun setupObservers() {
        binding.apply {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                    launch { viewModel.connectionState.collectLatest { onNetworkStateChanged(it) } }

                    launch {
                        viewModel.presetState.collectLatest { preset ->
                            presetAdapter.submitList(preset?.items?.take(3).orEmpty())
                        }
                    }

                    launch {
                        viewModel.themeState.collectLatest { theme ->
                            homeThemeAdapter.submitList(
                                buildList {
                                    add(ThemeUi.Custom())
                                    addAll(theme?.items?.take(2).orEmpty())
                                }
                            )
                        }
                    }

                    launch {
                        viewModel.bgBitmap.collectLatest { pair ->
                            val key = pair.first
                            val bmp = pair.second
                            bmp?.let {
                                when (key) {
                                    KEY_EDGE -> {
                                        binding.edgeToggle.isChecked = false
                                        viewModel.updateEdgeState { state -> state.copy(isEnableEdgeLighting = false) }
                                        WallpaperBgStore.saveFile(requireContext(), it)
                                        startActivity(Intent(requireContext(), EdgeWallpaperSettingsActivity::class.java))
                                    }

                                    KEY_RIPPLE -> {
                                        WallpaperBgStore.saveRippleAndNotify(requireContext(), it)
                                        startActivity(Intent(requireContext(), RippleWallpaperSettingsActivity::class.java))
                                    }
                                }
                            } ?: run {
                                Toast.makeText(requireContext(), getString(R.string.an_error_has_occurred), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    launch {
                        viewModel.restartOverlay.collectLatest { isRestart ->
                            if (isRestart) {
                                binding.edgeToggle.isChecked = true
                                viewModel.updateEdgeState { it.copy(isEnableEdgeLighting = true) }
                                startEdgeOverlay()
                            }
                        }
                    }
                }
            }
        }
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

        edgeToggle.setOnCheckedChangeListener { isSelected ->
            if (!viewModel.isToggleEdgeFirstTime) viewModel.isToggleEdgeFirstTime = true
            viewModel.updateEdgeState { it.copy(isEnableEdgeLighting = isSelected) }
            if (isSelected) startEdgeOverlay() else stopEdgeOverlay()
        }

        edgeToggle.setCheckedSilently(viewModel.edgeState.isEnableEdgeLighting)

        btnViewAllTheme.setOnClickListener {
            disableEdgeLighting()

            val intent = Intent(requireContext(), ThemesActivity::class.java)
            intent.putExtra(KEY_IS_CUSTOM, true)
            intent.putExtra(KEY_RECEIVE_DATA, TYPE_THEME)
            startActivity(intent)
        }

        btnViewAllPreset.setOnClickListener {
            disableEdgeLighting()

            val intent = Intent(requireContext(), ThemesActivity::class.java)
            intent.putExtra(KEY_IS_CUSTOM, false)
            intent.putExtra(KEY_RECEIVE_DATA, TYPE_PRESET)
            startActivity(intent)
        }
    }

    private fun disableEdgeLighting() {
        viewModel.updateEdgeState { state -> state.copy(isEnableEdgeLighting = false) }
        viewModel.saveCacheEdgeState()
        binding.edgeToggle.isChecked = false
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
//                disableEdgeLighting()

                binding.vpSettingEdge.post { binding.vpSettingEdge.updateHeightForCurrentPage() }
            }
        })
    }

    private fun startEdgeOverlay() {
        if (!Settings.canDrawOverlays(requireContext())) {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${requireContext().packageName}".toUri())
            overlayPermissionLauncher.launch(i)
            return
        }
        ContextCompat.startForegroundService(requireContext(), Intent(requireContext(), EdgeLightingOverlayService::class.java))
        viewModel.sendActionBroadcast(ACTION_EDGE_WALLPAPER_STATE_STOP)
    }

    private fun stopEdgeOverlay() {
        requireContext().stopService(Intent(requireContext(), EdgeLightingOverlayService::class.java))
    }

    private fun handleItemClick(item: Item) {
        when (item.type) {
            WallpaperType.TYPE_EDGE,
            WallpaperType.TYPE_STATIC -> {
                viewModel.onClickBackgroundUrl(item, requireContext().getFullScreenTargetSize())
            }

            WallpaperType.TYPE_VIDEO -> {
                viewModel.videoUrl = item.pathUrl
                startActivity(Intent(requireContext(), VideoWallpaperSettingsActivity::class.java))
                VideoPreloader.preload(requireContext().applicationContext, item.pathUrl)
            }

            WallpaperType.TYPE_RIPPLE -> {
                viewModel.rippleEffectUrl = item.pathUrl
                viewModel.loadBackgroundRippleUrl(item, requireContext().getFullScreenTargetSize())
            }
        }

        disableEdgeLighting()
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
