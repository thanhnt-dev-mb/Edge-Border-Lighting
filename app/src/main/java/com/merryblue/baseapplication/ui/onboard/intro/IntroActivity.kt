package com.merryblue.baseapplication.ui.onboard.intro

import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
        enableEdgeToEdge()
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, _ ->
            WindowInsetsCompat.CONSUMED
        }
        AdapterOpenAppManager.instance.registerDisableOpenAdsAt(IntroActivity::class.java)
    }
}
