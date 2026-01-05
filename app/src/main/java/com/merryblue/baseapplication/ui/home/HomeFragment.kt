package com.merryblue.baseapplication.ui.home

import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment
import android.os.Handler
import android.os.Looper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.BuildConfig
import com.merryblue.baseapplication.helpers.isAppInstalled
import com.merryblue.baseapplication.helpers.isBackground
import com.merryblue.baseapplication.helpers.openPolicy
import com.merryblue.baseapplication.ui.appupdate.ForceUpdateActivity
import com.merryblue.baseapplication.ui.iap.PurchaseActivity
import com.merryblue.baseapplication.ui.onboard.language.LanguageActivity
import com.merryblue.baseapplication.ui.setting.SettingActivity
import com.merryblue.baseapplication.ui.widget.BottomSheetRate
import org.app.core.base.binding.setOnSingleClickListener
import org.app.core.base.extensions.hide
import org.app.core.base.extensions.openActivityAndClearStack

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    override fun getLayoutId() = R.layout.fragment_home

    override fun initView(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectionState.collectLatest { connected ->
                    onNetworkStateChanged(connected)
                }
            }
        }
    }

    
    override fun setUpViews() {

    }
    
    override fun setupObservers() {
    }
}
