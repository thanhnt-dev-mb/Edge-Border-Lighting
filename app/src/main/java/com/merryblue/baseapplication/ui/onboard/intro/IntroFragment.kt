package com.merryblue.baseapplication.ui.onboard.intro

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.FragmentIntroBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment
import org.app.core.base.binding.setOnSingleClickListener
import org.app.core.base.extensions.setMargins

@AndroidEntryPoint
class IntroFragment : BaseFragment<FragmentIntroBinding>() {
    
    private val viewModel: IntroViewModel by activityViewModels()
    private var pageIndex = 0

    override
    fun getLayoutId() = R.layout.fragment_intro

    override fun initView(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.connectionState.collectLatest { connected ->
                    onNetworkStateChanged(connected)
                }
            }
        }
    }

    override fun getFragmentArguments() {
        arguments?.let {
            pageIndex = it.getInt(ARG_PAGE_NUMBER, 0)
            binding.data = viewModel.getPageDataBy(pageIndex, context ?: return)
        }

        if (pageIndex != 1 && !viewModel.isPremium()) {
            layoutCard = binding.layoutCard
            adsContainer = binding.adsContainer
        } else {
            binding.layoutCard.setMargins(0, 0, 0, 0)
        }
    }

    override
    fun setUpViews() {
        binding.nextBtn.setOnSingleClickListener {
            viewModel.goNextByPage(pageIndex)
        }
    }

    override
    fun setupObservers() {

    }

    companion object {
        private const val ARG_PAGE_NUMBER = "page_number"

        @JvmStatic
        fun newInstance(pageNumber: Int): IntroFragment {
            return IntroFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PAGE_NUMBER, pageNumber)
                }
            }
        }
    }
}
