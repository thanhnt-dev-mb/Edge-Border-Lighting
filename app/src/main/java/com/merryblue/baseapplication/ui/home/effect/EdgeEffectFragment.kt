package com.merryblue.baseapplication.ui.home.effect

import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.FragmentEdgeEffectBinding
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.helpers.mapFloatToRange
import com.merryblue.baseapplication.helpers.mapFloatToRangeLong
import com.merryblue.baseapplication.helpers.mapValueToProgress
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.view.CustomSeekBar
import com.merryblue.baseapplication.ui.view.CustomSeekBar.OnProgressChangeListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment
import kotlin.getValue

@AndroidEntryPoint
class EdgeEffectFragment : BaseFragment<FragmentEdgeEffectBinding>() {

    private val viewModel: EdgeEffectViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var effectAdapter: EdgeEffectAdapter

    override fun getLayoutId(): Int = R.layout.fragment_edge_effect

    override fun initView(view: View) {
        setupRecyclerView()
        applyInitialSlidersFromSaved()
        registerOnClick()
        viewModel.dispatch(EdgeEffectIntent.LoadInitial)
    }

    private fun setupRecyclerView() {
        effectAdapter = EdgeEffectAdapter { index ->
            viewModel.dispatch(EdgeEffectIntent.SelectEffect(index))
            homeViewModel.applySettingEdgeLighting()
        }

        binding.rcvEdgeEffect.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = effectAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    private fun applyInitialSlidersFromSaved() = binding.apply {
        val edgeState = viewModel.getEdgeState()
        val minSizePx = 2f.dpToPx
        val maxSizePx = 20f.dpToPx
        val minRadiusPx = 0f.dpToPx
        val maxRadiusPx = 60f.dpToPx

        seekBarSize.progress(edgeState.iconSizePx.mapValueToProgress(min = minSizePx, max = maxSizePx))
        seekBarSpeed.progress(1f - edgeState.speedMs.mapValueToProgress(min = 500L, max = 8000L))
        seekBarBottomRadius.progress(edgeState.bottomRadius.mapValueToProgress(min = minRadiusPx, max = maxRadiusPx))
        seekBarTopRadius.progress(edgeState.topRadius.mapValueToProgress(min = minRadiusPx, max = maxRadiusPx))
    }

    private fun registerOnClick() = binding.apply {
        seekBarSpeed.setOnProgressChangeListener(object : OnProgressChangeListener {
            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                if (!fromUser) return
                val durationMs = (1f - progress).mapFloatToRangeLong(min = 500L, max = 8000L)
                viewModel.dispatch(EdgeEffectIntent.UpdateSpeed(durationMs))
            }
            override fun onStopTrackingTouch(seekBar: CustomSeekBar) {
                homeViewModel.applySettingEdgeLighting()
            }
        })

        seekBarSize.setOnProgressChangeListener(object : OnProgressChangeListener {
            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                if (!fromUser) return
                val minPx = 2f.dpToPx
                val maxPx = 20f.dpToPx
                val sizePx = progress.mapFloatToRange(minPx, maxPx)
                viewModel.dispatch(EdgeEffectIntent.UpdateSize(sizePx))
            }
            override fun onStopTrackingTouch(seekBar: CustomSeekBar) {
                homeViewModel.applySettingEdgeLighting()
            }
        })

        seekBarBottomRadius.setOnProgressChangeListener(object : OnProgressChangeListener {
            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                if (!fromUser) return
                val radiusPx = progress.mapFloatToRange(0f.dpToPx, 60f.dpToPx)
                viewModel.dispatch(EdgeEffectIntent.UpdateBottomRadius(radiusPx))
            }
            override fun onStopTrackingTouch(seekBar: CustomSeekBar) {
                homeViewModel.applySettingEdgeLighting()
            }
        })

        seekBarTopRadius.setOnProgressChangeListener(object : OnProgressChangeListener {
            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                if (!fromUser) return
                val radiusPx = progress.mapFloatToRange(0f.dpToPx, 60f.dpToPx)
                viewModel.dispatch(EdgeEffectIntent.UpdateTopRadius(radiusPx))
            }
            override fun onStopTrackingTouch(seekBar: CustomSeekBar) {
                homeViewModel.applySettingEdgeLighting()
            }
        })
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe state changes
                launch {
                    viewModel.state.collect { state ->
                        effectAdapter.submitList(state.listEffect)
                    }
                }

                // Observe effects (if any in the future)
                launch {
                    viewModel.effect.collect { effect ->
                        // Handle effects here if needed
                    }
                }
            }
        }
    }
}