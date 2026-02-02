package com.merryblue.baseapplication.ui.setting

import android.content.Intent
import android.provider.Settings
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.BuildConfig
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.coredata.model.Setting
import com.merryblue.baseapplication.databinding.FragmentSettingBinding
import com.merryblue.baseapplication.helpers.isAppInstalled
import com.merryblue.baseapplication.helpers.openPolicy
import com.merryblue.baseapplication.service.EdgeLightingOverlayService
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.iap.PurchaseActivity
import com.merryblue.baseapplication.ui.onboard.language.LanguageActivity
import com.merryblue.baseapplication.ui.policy.PolicyActivity
import com.merryblue.baseapplication.ui.widget.BottomSheetRate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment
import org.app.core.base.OnItemClickListener
import org.app.core.base.binding.setOnSingleClickListener
import org.app.core.base.extensions.navigateSafe
import org.app.core.base.extensions.openActivity
import org.app.core.base.extensions.setupVertical

@AndroidEntryPoint
class SettingFragment : BaseFragment<FragmentSettingBinding>() {
    private val prefs by lazy { AppPreferences(requireContext()) }
    private val viewModel: SettingViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private var adapter: SettingAdapter? = null

    private val overlayPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(requireContext())) {
            startEdgeOverlay()
        } else {
            binding.edgeToggle.isChecked = false
        }
    }

    override fun onResume() {
        super.onResume()
        binding.edgeToggle.isChecked = prefs.edgeState.isEnableEdgeLighting
    }
    
    override fun getLayoutId() = R.layout.fragment_setting

    override fun initView(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectionState.collectLatest { connected ->
                    onNetworkStateChanged(connected)
                }
            }
        }
    }

    override fun setBindingVariables() {
        binding.viewModel = viewModel
    }
    
    override fun setUpViews() {
        binding.premiumLayout.setOnSingleClickListener {
            if (viewModel.isPremium() || context == null) return@setOnSingleClickListener

            PurchaseActivity.open(requireContext(), "setting")
        }
        setupRecycler()
        registerListener()
    }

    private fun startEdgeOverlay() {
        if (!Settings.canDrawOverlays(requireContext())) {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${requireContext().packageName}".toUri())
            overlayPermissionLauncher.launch(i)
            return
        }
        ContextCompat.startForegroundService(requireContext(), Intent(requireContext(), EdgeLightingOverlayService::class.java))
    }

    private fun stopEdgeOverlay() {
        requireContext().stopService(Intent(requireContext(), EdgeLightingOverlayService::class.java))
    }

    private fun registerListener() {
        binding.apply {
            edgeToggle.setOnCheckedChangeListener { isSelected ->
                homeViewModel.updateEdgeState { it.copy(isEnableEdgeLighting = isSelected) }
                if (isSelected) startEdgeOverlay() else stopEdgeOverlay()
            }

            edgeToggle.setCheckedSilently(homeViewModel.edgeState.isEnableEdgeLighting)
        }
    }

    override fun onFragmentResume() {
        val items = viewModel.getSettingItems(context ?: return)
        adapter?.notifyDataChanged(items)
        binding.premiumTitle.text = viewModel.getPremiumTitle(context ?: return)
    }
    
    override fun onFragmentPause() {
    }
    
    private fun setupRecycler() {
        adapter = SettingAdapter(emptyList()).apply {
            onItemClickListener = object : OnItemClickListener<Setting> {
                override fun onItemClick(viewItem: View?, data: Setting, position: Int, action: String?) {
                    handleItemClick(data, viewItem)
                }
            }
        }
        binding.settingRv.setupVertical(adapter ?: return)
    }

    private fun handleItemClick(data: Setting, view: View? = null) {
        when(data.code) {
            Setting.Code.LANGUAGE -> {
                LanguageActivity.open(context ?: return, "setting")
            }
            Setting.Code.RATE -> {
                try {
                    (childFragmentManager.findFragmentByTag(BottomSheetRate.TAG) as? BottomSheetDialogFragment)?.dismissAllowingStateLoss()
                    val bottom = BottomSheetRate {

                    }
                    bottom.show(childFragmentManager, BottomSheetRate.TAG)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            Setting.Code.Share -> {
                val appId = BuildConfig.APPLICATION_ID
                val shareLink = "https://play.google.com/store/apps/details?id=$appId"
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareLink)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(
                    sendIntent, getString(R.string.txt_share_msg, getString(R.string.app_name))
                )
                startActivity(shareIntent)
            }
            Setting.Code.FEEDBACK -> {
                context ?: return
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback.developer.app@gmail.com"))
                intent.putExtra(Intent.EXTRA_SUBJECT, requireContext().getString(R.string.app_name))
                val gmailPkg = "com.google.android.gm"
                val isGmailInstalled = requireContext().isAppInstalled(gmailPkg)
                if (isGmailInstalled) {
                    intent.type = "text/html"
                    intent.setPackage(gmailPkg)
                    startActivity(intent);
                } else {
                    intent.type = "message/rfc822"
                    if (intent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(intent)
                    } else {
                        startActivity(Intent.createChooser(intent, "Choose an Email application to start"))
                    }
                }
            }
            Setting.Code.PRIVACY -> {
                context?.openPolicy()
            }
            else -> {}
        }
    }
}