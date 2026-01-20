package com.merryblue.baseapplication.ui.home.color

import android.view.View
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.edge.EdgePreset
import com.merryblue.baseapplication.coredata.model.edge.EdgeSelection
import com.merryblue.baseapplication.coredata.model.edge.EdgeStyle
import com.merryblue.baseapplication.databinding.FragmentEdgeColorBinding
import com.merryblue.baseapplication.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
            viewModel.dispatch(EdgeColorIntent.SelectColor(index))
        }

        binding.rcvEdgeColor.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = edgeColorAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }

        viewModel.loadInitial()
        registerOnClick()
    }

    private fun registerOnClick() = with(binding) {
        btnFourColors.setOnClickListener { viewModel.dispatch(EdgeColorIntent.SelectTab(EdgeTab.TAB_4)) }
        btnThreeColors.setOnClickListener { viewModel.dispatch(EdgeColorIntent.SelectTab(EdgeTab.TAB_3)) }
        btnTwoColors.setOnClickListener { viewModel.dispatch(EdgeColorIntent.SelectTab(EdgeTab.TAB_2)) }
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

                // 1) Update tab UI when selectedTab changes (IMPORTANT)
                launch {
                    viewModel.state
                        .map { it.selectedTab }
                        .distinctUntilChanged()
                        .collect { tab ->
                            renderSelectedTab(tab)
                        }
                }

                // 2) Update list + emit preset when selectedIndex changes
                launch {
                    viewModel.state
                        .map { Triple(it.selectedTab, it.selectedIndex, it.items) }
                        .distinctUntilChanged { old, new ->
                            old.first == new.first && old.second == new.second && old.third == new.third
                        }
                        .collect { (tab, index, items) ->
                            edgeColorAdapter.submitList(items)

                            if (index in items.indices) {
                                val selected = items[index]

                                homeViewModel.emitEdgeColor(
                                    EdgeSelection.EdgeColor(
                                        tab = tab,
                                        selectedIndex = index,
                                        preset = EdgePreset.BackgroundColor(
                                            color = R.color.colorBgSurface,
                                            edge = EdgeStyle.LinearColor(colors = selected.colors)
                                        )
                                    )
                                )

                                viewModel.updateEdgeState { it.copy(edgeStyleType = 0, colors = selected.colors) }
                            }
                        }
                }
            }
        }
    }
}