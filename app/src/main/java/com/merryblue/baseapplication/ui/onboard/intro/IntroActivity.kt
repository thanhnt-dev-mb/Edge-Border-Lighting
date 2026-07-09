package com.merryblue.baseapplication.ui.onboard.intro

import androidx.activity.viewModels
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityIntroBinding
import com.merryblue.baseapplication.ui.iap.PurchaseActivity
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.ads.openads.AdapterOpenAppManager
import org.app.core.base.BaseActivity

@AndroidEntryPoint
class IntroActivity : BaseActivity<ActivityIntroBinding>() {
    private val viewModel: IntroViewModel by viewModels()

    override
    fun getLayoutId() = R.layout.activity_intro

    override fun setUpViews() {
        enableEdgeToEdge(binding.main, true)
        AdapterOpenAppManager.instance.registerDisableOpenAdsAt(IntroActivity::class.java)

        super.setUpViews()
    }

    override fun onCloseAction() {
        viewModel.setFirstTime(false)
        PurchaseActivity.open(this, "onboard")
    }
}
