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

    override fun getLayoutId() = R.layout.fragment_native_fullscreen

    override fun setUpViews() {
        binding.closeBtn.hide()
        binding.closeBtn.setOnSingleClickListener {
            viewModel.setCurrentPage(3)
        }
    }

    override fun onFragmentResume() {
        Timber.i("###DEBUG -> onFragmentResume $TAG")
        setupAds()
    }

    override fun onFragmentPause() {
        CoreAds.instance.nativeContainer = null
    }

    private fun setupAds() {
        val actv = activity ?: return
        val remoteConfig = viewModel.getRemoteConfiguration()
        if (remoteConfig == null) {
            viewModel.setCurrentPage(3)
            return
        }

        val tagNative = TAG + "_Native"
        val tagNativeNA = TAG + "_Native_NA"
        val nativeAds = remoteConfig.natives?.firstOrNull {
            it.tag == tagNative || it.tag == tagNativeNA
        }

        if (nativeAds != null && !nativeAds.id.isNullOrBlank()) {
            binding.closeBtn.show()
            if (nativeAds.tag == tagNative) {
                CoreAds.instance.showAdapterNativeAdsMultiple(
                    actv.applicationContext,
                    actv,
                    binding.adsContainer,
                    nativeAds.id!!,
                    nativeAds.event ?: tagNative,
                    NativeStyle.FULLSCREEN,
                    false,
                    nativeAds.preload ?: 0
                )
            } else {
                CoreAds.instance.showAdapterNativeAdsIfAvailable(
                    actv.applicationContext,
                    actv,
                    binding.adsContainer,
                    nativeAds.id!!,
                    nativeAds.event ?: tagNativeNA,
                    style = NativeStyle.FULLSCREEN
                )
            }
        } else {
            viewModel.setCurrentPage(3)
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