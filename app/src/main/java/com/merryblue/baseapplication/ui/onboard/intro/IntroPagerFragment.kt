package com.merryblue.baseapplication.ui.onboard.intro

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.FragmentIntroPagerBinding
import com.merryblue.baseapplication.enums.InterstitialFunction
import com.merryblue.baseapplication.enums.IntroPage
import com.merryblue.baseapplication.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.ads.CoreAds
import org.app.core.ads.callback.AdsCallback
import org.app.core.base.BaseFragment
import org.app.core.base.extensions.openActivityAndClearStack
import org.app.core.base.utils.StringResId

@AndroidEntryPoint
class IntroPagerFragment : BaseFragment<FragmentIntroPagerBinding>() {

    private val viewModel: IntroViewModel by activityViewModels()
    private var isGoHome: Boolean = false

    private var pageChangedCallback = object : ViewPager2.OnPageChangeCallback() {
        override
        fun onPageSelected(position: Int) {
            viewModel.setCurrentPage(position)
        }
    }

    override
    fun getLayoutId() = R.layout.fragment_intro_pager

    override fun initView(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.connectionState.collectLatest { connected ->
                    onNetworkStateChanged(connected)
                }
            }
        }
    }

    override
    fun setBindingVariables() {
        binding.viewModel = viewModel
    }

    override fun setUpViews() {
        setupPager()
    }

    override fun setupObservers() {
        viewModel.openHomeEvent.observe(this) {
            showInterstitialIfNeed()
        }
        
        viewModel.currentPage.observe(this) {
            if (it == 0) {
                binding.introPager.setCurrentItem(it, false)
            } else {
                binding.introPager.setCurrentItem(it, true)
            }
        }
    }

    override fun onFragmentResume() {
        binding.introPager.registerOnPageChangeCallback(pageChangedCallback)
    }

    override fun onFragmentPause() {
        binding.introPager.unregisterOnPageChangeCallback(pageChangedCallback)
    }

    private fun setupPager() {
        val pagerAdapter = IntroPagerAdapter(requireActivity().supportFragmentManager, lifecycle)
        binding.introPager.apply {
            adapter = pagerAdapter
            isUserInputEnabled = false
        }
    }

    private fun showInterstitialIfNeed() {
        val rmConfig = viewModel.getRemoteConfiguration()
        val ads = rmConfig?.interstitials?.firstOrNull {
            it.tag == InterstitialFunction.Guide.name
        }

        if (ads?.id.isNullOrBlank()) {
            openHome()
            return
        }
        activity?.let { actv ->
            isGoHome = false
            CoreAds.instance.showAdapterInterstitialAds(
                timelapse = 30000,
                getString(StringResId.loadingAds),
                actv,
                ads?.id!!,
                ads.event ?: "ClickGuideDummy",
                object : AdsCallback() {
                    override fun onClosed() {
                        super.onClosed()
                        openHome()
                    }
                    
                    override fun onError(message: String?) {
                        super.onError(message)
                        openHome()
                    }
                })
        } ?: kotlin.run {
            openHome()
        }
    }
    
    private fun openHome() {
        activity ?: return

        if (isGoHome || requireActivity().isFinishing || requireActivity().isDestroyed) return

        isGoHome = true
        viewModel.setFirstTime(false)
        openActivityAndClearStack(HomeActivity::class.java)
    }
    
    inner class IntroPagerAdapter(
        fm: FragmentManager,
        lifecycle: Lifecycle
    ) : FragmentStateAdapter(fm, lifecycle) {
        
        override fun getItemCount(): Int {
            return IntroPage.allPage(viewModel.hideNativeFullPage()).size
        }
        
        override fun createFragment(position: Int): Fragment {
            val page = IntroPage.allPage(viewModel.hideNativeFullPage()).getOrNull(position)
            return if (page  == IntroPage.PAGE_ADS) {
                NativeFullscreenFragment.newInstance(position)
            } else {
                IntroFragment.newInstance(position)
            }
        }
    }
}
