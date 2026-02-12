package com.merryblue.baseapplication.ui.onboard.intro

import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityIntroBinding
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.ads.openads.AdapterOpenAppManager
import org.app.core.base.BaseActivity

@AndroidEntryPoint
class IntroActivity : BaseActivity<ActivityIntroBinding>() {

    override
    fun getLayoutId() = R.layout.activity_intro

    override fun setUpViews() {
        enableEdgeToEdge(binding.main, true)
        AdapterOpenAppManager.instance.registerDisableOpenAdsAt(IntroActivity::class.java)
    }

    override fun setupBinding() {
        //TODO: Should do nothing
    }
}
