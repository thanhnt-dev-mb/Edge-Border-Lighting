package com.merryblue.baseapplication.ui.policy

import android.view.View
import androidx.fragment.app.viewModels
import androidx.webkit.WebViewAssetLoader
import com.merryblue.base.ui.policy.LocalContentWebViewClient
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.FragmentPolicyLayoutBinding
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseFragment
import org.app.core.base.binding.setOnSingleClickListener
import org.app.core.base.extensions.getMyColor
import org.app.core.base.utils.StringResId

@AndroidEntryPoint
class PolicyFragment : BaseFragment<FragmentPolicyLayoutBinding>() {

    private val viewModel: PolicyViewModel by viewModels()

    override
    fun getLayoutId() = R.layout.fragment_policy_layout

    override
    fun setBindingVariables() {
        binding.viewModel = viewModel
    }

    override
    fun setUpViews() {
        binding.backBtn.setOnSingleClickListener {
            activity?.finish()
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(requireContext()))
            .build()
        binding.webView.webViewClient = LocalContentWebViewClient(assetLoader)

        binding.webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }
    
    override fun onFragmentResume() {
        binding.webView.visibility = View.VISIBLE
    }
}
