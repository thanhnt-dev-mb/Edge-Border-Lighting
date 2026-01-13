package com.merryblue.baseapplication.ui.home

import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.edge.DisplayHole
import com.merryblue.baseapplication.coredata.model.edge.DisplayInfinity
import com.merryblue.baseapplication.coredata.model.edge.DisplayNotch
import com.merryblue.baseapplication.coredata.model.edge.DisplayNotchType
import com.merryblue.baseapplication.coredata.model.edge.EdgeSelection
import com.merryblue.baseapplication.coredata.model.edge.EdgeSettings
import com.merryblue.baseapplication.databinding.FragmentHomeBinding
import com.merryblue.baseapplication.helpers.updateHeightForCurrentPage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var mediator: TabLayoutMediator

    override fun getLayoutId() = R.layout.fragment_home

    override fun setUpViews() {
        initViewPager()
        initTabLayout()
        registerOnClick()
    }

    private fun registerOnClick() = with (binding) {
        edgeToggle.setOnCheckedChangeListener { edgeView.visibility = if (it) View.VISIBLE else View.INVISIBLE }
    }

    private fun initViewPager() = with(binding) {
        vpSettingEdge.adapter = SettingEdgeAdapter(this@HomeFragment)
    }

    private fun initTabLayout() {
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
