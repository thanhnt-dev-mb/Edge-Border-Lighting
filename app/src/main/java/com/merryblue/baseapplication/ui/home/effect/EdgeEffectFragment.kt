package com.merryblue.baseapplication.ui.home.effect

import android.view.View
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
import com.merryblue.baseapplication.databinding.FragmentEdgeEffectBinding
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment

@AndroidEntryPoint
class EdgeEffectFragment : BaseFragment<FragmentEdgeEffectBinding>() {
    private val viewModel: EdgeEffectViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var effectAdapter: EdgeEffectAdapter

    override fun getLayoutId(): Int = R.layout.fragment_edge_effect

    override fun initView(view: View) {
        effectAdapter = EdgeEffectAdapter { index ->
            viewModel.loadEffect(index)
        }

        binding.rcvEdgeEffect.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = effectAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }

        viewModel.loadEffect(0)
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { effectState ->
                        val index = effectState.selectedIndex
                        val items = effectState.listEffect
                        effectAdapter.submitList(items)
                        if (index in items.indices) {
                            val selected = items[index]
                            homeViewModel.emitEdgeColor(
                                EdgeSelection.EdgeEffect(
                                    selectedIndex = index,
                                    preset = EdgePreset.BackgroundColor(
                                        color = R.color.colorBgSurface,
                                        edge = EdgeStyle.Pattern(
                                            vectorResId = selected.resId,
                                            iconSizePx = 8f.dpToPx,
                                            advancePx = 18f.dpToPx,
                                            rotate = true,
                                            phaseMultiplier = 0.1f
                                        )
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