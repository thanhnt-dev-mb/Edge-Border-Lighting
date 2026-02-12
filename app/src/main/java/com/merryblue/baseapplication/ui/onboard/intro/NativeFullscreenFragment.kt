package com.merryblue.baseapplication.ui.onboard.intro

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.FragmentNativeFullscreenBinding
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.ads.CoreAds
import org.app.core.ads.base.NativeStyle
import org.app.core.base.BaseFragment
import org.app.core.base.binding.setOnSingleClickListener
import org.app.core.base.extensions.hide
import org.app.core.base.extensions.show
import timber.log.Timber


@AndroidEntryPoint
class NativeFullscreenFragment : BaseFragment<FragmentNativeFullscreenBinding>() {
    private val viewModel: IntroViewModel by activityViewModels()

    private var pageIndex : Int = 1

    override val showInitializeLoading: Boolean
        get() = false

    override var nativeHeight: Int = -1

    override fun getLayoutId() = R.layout.fragment_native_fullscreen

    override fun getFragmentArguments() {
        arguments?.let {
            pageIndex = it.getInt(ARG_PAGE_NUMBER, 0)
        }
    }

    override fun setUpViews() {
        binding.closeBtn.setOnSingleClickListener {
            viewModel.goNextByPage(pageIndex)
        }
    }

    companion object {
        private const val ARG_PAGE_NUMBER = "page_number"

        @JvmStatic
        fun newInstance(pageNumber: Int): NativeFullscreenFragment {
            return NativeFullscreenFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PAGE_NUMBER, pageNumber)
                }
            }
        }
    }
}