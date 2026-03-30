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
import org.app.core.ads.remoteconfig.CoreRemoteConfig
import org.app.core.base.BaseFragment
import org.app.core.base.binding.setOnSingleClickListener
import org.app.core.base.extensions.getMyColor
import org.app.core.base.extensions.setMargins

@AndroidEntryPoint
class IntroFragment : BaseFragment<FragmentIntroBinding>() {
    
    private val viewModel: IntroViewModel by activityViewModels()
    private var pageIndex = 0

    override val nativeHeight: Int
        get() = -1

    override val showInitializeLoading: Boolean
        get() = false

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
    }

    override fun setBindingVariables() {
        val remoteConfig = viewModel.getRemoteConfiguration()
        if (remoteConfig == null || remoteConfig.status == false) {
            return
        }

        val tagNative = TAG + "_Native_$pageIndex"
        val nativeAds = remoteConfig.natives?.firstOrNull {
            it.tag == tagNative && !it.id.isNullOrBlank()
        }

        if (nativeAds != null && !viewModel.isPremium()) {
            layoutCard = binding.layoutCard
            adsContainer = binding.adsContainer
            binding.adsContainer.setBackgroundColor(getMyColor(R.color.colorWhite))
        } else {
            binding.layoutCard.setMargins(0, 0, 0, 0)
            binding.adsContainer.setBackgroundColor(getMyColor(R.color.color_19193f))
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
