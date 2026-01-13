package com.merryblue.baseapplication.ui.home

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.merryblue.baseapplication.R
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
        initTabLayout()
        registerOnClick()
    }

    private fun registerOnClick() = with (binding) {
        edgeToggle.setOnCheckedChangeListener {
            viewModel.emitVisibilityEdgeView(it)
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
