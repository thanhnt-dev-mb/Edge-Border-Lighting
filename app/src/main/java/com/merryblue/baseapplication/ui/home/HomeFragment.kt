package com.merryblue.baseapplication.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
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
import com.merryblue.baseapplication.helpers.BackgroundType.BACKGROUND_URL
import com.merryblue.baseapplication.helpers.EDGE_MOST
import com.merryblue.baseapplication.helpers.KEY_RECEIVE_DATA
import com.merryblue.baseapplication.helpers.RIPPLE_MAGICAL_BORDERS
import com.merryblue.baseapplication.helpers.TYPE_PRESET
import com.merryblue.baseapplication.helpers.TYPE_THEME
import com.merryblue.baseapplication.helpers.updateHeightForCurrentPage
import com.merryblue.baseapplication.service.EdgeLightingOverlayService
import com.merryblue.baseapplication.ui.theme.ThemesActivity
import com.merryblue.baseapplication.ui.wallpaper.EdgeWallpaperSettingsActivity
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

    private val presetOnClick: (Item) -> Unit = { item ->
        viewModel.applyEdgeState { it.copy(backgroundType = BACKGROUND_URL, backgroundImageUrl = item.pathUrl) }
        startActivity(Intent(requireContext(), EdgeWallpaperSettingsActivity::class.java))
    }

    private val customOnClick: () -> Unit = {

    }

    private val overlayPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(requireContext())) {
            startEdgeOverlay()
        } else {
            binding.edgeToggle.isChecked = false
        }
    }

    override fun getLayoutId() = R.layout.fragment_home

    override fun setUpViews() {
        viewModel.loadPreset(EDGE_MOST)
        viewModel.loadThemes(RIPPLE_MAGICAL_BORDERS)

        initTabLayout()
        initRecyclerView()
        registerOnClick()
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
//            viewModel.emitVisibilityEdgeView(isSelected)
            viewModel.updateEdgeState { it.copy(isEnableEdgeLighting = isSelected) }
            if (isSelected) startEdgeOverlay() else stopEdgeOverlay()
        }

        edgeToggle.setCheckedSilently(viewModel.getEdgeState().isEnableEdgeLighting)

        btnViewAllTheme.setOnClickListener {
            val intent = Intent(requireContext(), ThemesActivity::class.java)
            intent.putExtra(KEY_RECEIVE_DATA, TYPE_THEME)
            startActivity(intent)
        }

        btnViewAllPreset.setOnClickListener {
            val intent = Intent(requireContext(), ThemesActivity::class.java)
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
                }
            }
        }
    }

    private fun startEdgeOverlay() {
        if (!Settings.canDrawOverlays(requireContext())) {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}"))
            overlayPermissionLauncher.launch(i)
            return
        }
        ContextCompat.startForegroundService(requireContext(), Intent(requireContext(), EdgeLightingOverlayService::class.java))
    }

    private fun stopEdgeOverlay() {
        requireContext().stopService(Intent(requireContext(), EdgeLightingOverlayService::class.java))
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
