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
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment
import kotlin.Int

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

        viewModel.loadInitial(EdgeTab.TAB_4)
        registerOnClick()
    }

    private fun registerOnClick() = with(binding) {

        val tabButtons = listOf(btnFourColors, btnThreeColors, btnTwoColors)
        val checkIcons = listOf(ivCheckFour, ivCheckThree, ivCheckTwo)

        fun selectTab(selectedBtn: View, selectedCheck: AppCompatImageView, tab: EdgeTab) {
            tabButtons.forEach { it.setBackgroundResource(R.drawable.bg_tab_color_edge_unselected) }
            checkIcons.forEach { it.setImageResource(R.drawable.ic_check_unselected) }

            selectedBtn.setBackgroundResource(R.drawable.bg_tab_color_edge_selected)
            selectedCheck.setImageResource(R.drawable.ic_check_selected)

            viewModel.dispatch(EdgeColorIntent.SelectTab(tab))
        }

        btnFourColors.setOnClickListener { selectTab(btnFourColors, ivCheckFour, EdgeTab.TAB_4) }
        btnThreeColors.setOnClickListener { selectTab(btnThreeColors, ivCheckThree, EdgeTab.TAB_3) }
        btnTwoColors.setOnClickListener { selectTab(btnTwoColors, ivCheckTwo, EdgeTab.TAB_2) }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.map { Triple(it.selectedTab, it.selectedIndex, it.items) }
                        .distinctUntilChangedBy { it.first to it.second }
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
                            }
                        }
                }
            }
        }
    }
}