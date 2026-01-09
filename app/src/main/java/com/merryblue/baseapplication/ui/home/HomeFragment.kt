package com.merryblue.baseapplication.ui.home

import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.coredata.model.edge.EdgeAdvanced
import com.merryblue.baseapplication.coredata.model.edge.EdgePreset
import com.merryblue.baseapplication.coredata.model.edge.EdgeSelection
import com.merryblue.baseapplication.coredata.model.edge.EdgeSettings
import com.merryblue.baseapplication.coredata.model.edge.EdgeStyle
import com.merryblue.baseapplication.databinding.FragmentHomeBinding
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.helpers.updateHeightForCurrentPage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
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
        edgeToggle.setOnCheckedChangeListener { isToggle ->
            edgeView.visibility = if (isToggle) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun initViewPager() = with(binding) {
        vpSettingEdge.adapter = SettingEdgeAdapter(this@HomeFragment)
    }

    private fun initTabLayout() {
        val listSetting = buildList {
            add(getString(R.string.txt_color))
            add(getString(R.string.txt_effect))
            add(getString(R.string.txt_direction))
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.connectionState.collectLatest { connected ->
                        onNetworkStateChanged(connected)
                    }
                }

                launch {
                    viewModel.edgeColorEvents.drop(1).collect { edgeSelection ->
                        when (edgeSelection) {
                            is EdgeSelection.EdgeColor -> {
                                binding.edgeView.applyPreset(edgeSelection.preset)
                            }

                            is EdgeSelection.EdgeEffect -> {
                                if (edgeSelection.selectedIndex == 0) {
                                    binding.edgeView.setPatternEnabled(false)
                                } else binding.edgeView.applyPreset(edgeSelection.preset)
                            }

                            is EdgeSelection.EdgeAdvanced -> {

                            }
                        }
                    }
                }

                launch {
                    viewModel.edgeSettingsEvents.drop(1).collect { edgeSettings ->
                        when (edgeSettings) {
                            is EdgeSettings.EdgeSpeed -> binding.edgeView.setSpeedMs(edgeSettings.progress)
                            is EdgeSettings.EdgeSize -> binding.edgeView.setSizePx(edgeSettings.progress)
                            is EdgeSettings.EdgeBottomRadius -> binding.edgeView.setBottomRadiusPx(edgeSettings.progress)
                            is EdgeSettings.EdgeTopRadius -> binding.edgeView.setTopRadiusPx(edgeSettings.progress)
                        }
                    }
                }

                launch {
                    viewModel.edgeAdvancedEvents.drop(1).collect { adv ->
                        binding.edgeView.setAdvanced(adv)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        if (::mediator.isInitialized) mediator.detach()
        super.onDestroyView()
    }
}
