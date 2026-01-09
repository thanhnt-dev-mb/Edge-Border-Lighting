package com.merryblue.baseapplication.ui.home.advanced

import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.FragmentEdgeAdvancedBinding
import com.merryblue.baseapplication.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment


@AndroidEntryPoint
class EdgeAdvancedFragment : BaseFragment<FragmentEdgeAdvancedBinding>() {

    private val viewModel: EdgeAdvancedViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var directionAdapter: EdgeDirectionAdapter
    private lateinit var notchTypeAdapter: EdgeNotchTypeAdapter

    override fun getLayoutId(): Int = R.layout.fragment_edge_advanced

    override fun initView(view: View) {
        directionAdapter = EdgeDirectionAdapter { viewModel.selectDirection(it) }
        notchTypeAdapter = EdgeNotchTypeAdapter { viewModel.selectNotchType(it) }

        binding.apply {
            rcvDirection.apply {
                adapter = directionAdapter
                layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
                itemAnimator = null
            }

            rcvNotchType.apply {
                adapter = notchTypeAdapter
                layoutManager = GridLayoutManager(requireContext(), 4)
                setHasFixedSize(true)
                itemAnimator = null
            }
        }

        viewModel.selectDirection(0)
        viewModel.selectNotchType(0)
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.directionStateFlow.collect { state ->
                        val directionIndex = state.directionSelectedIndex
                        val directionItems = state.listDirection
                        directionAdapter.submitList(directionItems)
                        if (directionIndex in directionItems.indices) {
                            val selected = directionItems[directionIndex]
                            homeViewModel.emitEdgeDirection(selected.type)
                        }
                    }
                }

                launch {
                    viewModel.notchTypeStateFlow.collect { state ->
                        val notchTypeIndex = state.notchTypeSelectedIndex
                        val notchTypeItems = state.listNotchType
                        notchTypeAdapter.submitList(notchTypeItems)
                        if (notchTypeIndex in notchTypeItems.indices) {
                            val selected = notchTypeItems[notchTypeIndex]
                            homeViewModel.emitEdgeAdvances(selected.type)
                        }
                    }
                }
            }
        }
    }
}