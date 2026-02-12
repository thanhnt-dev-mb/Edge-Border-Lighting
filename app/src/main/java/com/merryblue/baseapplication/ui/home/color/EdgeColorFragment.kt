package com.merryblue.baseapplication.ui.home.color

import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.FragmentEdgeColorBinding
import com.merryblue.baseapplication.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment

@AndroidEntryPoint
class EdgeColorFragment: BaseFragment<FragmentEdgeColorBinding>() {
    fun color(@ColorRes id: Int) = ContextCompat.getColor(requireContext(), id)
    private val viewModel: EdgeColorViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var edgeColorAdapter: EdgeColorAdapter

    override fun getLayoutId(): Int = R.layout.fragment_edge_color

    override fun initView(view: View) {
        edgeColorAdapter = EdgeColorAdapter { index ->
            homeViewModel.applySettingEdgeLighting()
            viewModel.dispatch(EdgeColorIntent.SelectColor(index))
        }

        binding.rcvEdgeColor.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = edgeColorAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }

        viewModel.dispatch(EdgeColorIntent.LoadInitial)
        registerOnClick()
    }

    private fun registerOnClick() = with(binding) {
        btnFourColors.setOnClickListener {
            homeViewModel.applySettingEdgeLighting()
            viewModel.dispatch(EdgeColorIntent.SelectTab(EdgeTab.TAB_4))
        }
        btnThreeColors.setOnClickListener {
            homeViewModel.applySettingEdgeLighting()
            viewModel.dispatch(EdgeColorIntent.SelectTab(EdgeTab.TAB_3))
        }
        btnTwoColors.setOnClickListener {
            homeViewModel.applySettingEdgeLighting()
            viewModel.dispatch(EdgeColorIntent.SelectTab(EdgeTab.TAB_2))
        }
    }

    private fun renderSelectedTab(tab: EdgeTab) = with(binding) {
        val tabButtons = mapOf(
            EdgeTab.TAB_4 to btnFourColors,
            EdgeTab.TAB_3 to btnThreeColors,
            EdgeTab.TAB_2 to btnTwoColors,
        )
        val checkIcons = mapOf(
            EdgeTab.TAB_4 to ivCheckFour,
            EdgeTab.TAB_3 to ivCheckThree,
            EdgeTab.TAB_2 to ivCheckTwo,
        )

        // reset
        tabButtons.values.forEach { it.setBackgroundResource(R.drawable.bg_tab_color_edge_unselected) }
        checkIcons.values.forEach { it.setImageResource(R.drawable.ic_check_unselected) }

        // select current
        tabButtons[tab]?.setBackgroundResource(R.drawable.bg_tab_color_edge_selected)
        checkIcons[tab]?.setImageResource(R.drawable.ic_check_selected)
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.state.collect { state ->
                        renderSelectedTab(state.selectedTab)
                        edgeColorAdapter.submitList(state.items)
                    }
                }

                launch {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is EdgeColorEffect.ApplyColors -> {
                                viewModel.applyColorsToSystem(effect.colors)
                            }
                        }
                    }
                }
            }
        }
    }

}