package com.merryblue.baseapplication.ui.home.color

import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.EdgeColorItem
import com.merryblue.baseapplication.databinding.FragmentEdgeColorBinding
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment

class EdgeColorFragment: BaseFragment<FragmentEdgeColorBinding>() {
    fun color(@ColorRes id: Int) = ContextCompat.getColor(requireContext(), id)
    private val viewModel: EdgeColorViewModel by viewModels()
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

        val data = listOf(
            EdgeColorItem(intArrayOf(
                color(R.color.color_edge_blue_00F0FF),
                color(R.color.color_edge_blue_00A3FF),
                color(R.color.color_edge_purple_AD00FF),
            )),
            EdgeColorItem(intArrayOf(
                color(R.color.color_edge_blue_0066FF),
                color(R.color.color_edge_black_17192B),
                color(R.color.color_edge_blue_0066FF),
            )),
            EdgeColorItem(intArrayOf(
                color(R.color.color_edge_pink_FF00F5),
                color(R.color.color_edge_blue_00D0FF),
                color(R.color.color_edge_green_00FF1E),
                color(R.color.color_edge_yellow_EEFF00),
            ))
        )

//        viewModel.setInitialData(data, defaultIndex = 0)
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
//                        edgeColorAdapter.submitList(state.items)
//                        if (state.selectedIndex != -1) {
//                            val selected = state.items[state.selectedIndex]
//                            Timber.tag("Log_Selected").d("selected: $selected")
//                        }
                    }
                }
            }

        }
    }
}