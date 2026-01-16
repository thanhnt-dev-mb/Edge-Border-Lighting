package com.merryblue.baseapplication.ui.theme

import android.os.Bundle
import com.google.android.material.tabs.TabLayoutMediator
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityThemesBinding
import com.merryblue.baseapplication.helpers.EDGE_FIM
import com.merryblue.baseapplication.helpers.EDGE_MOST
import com.merryblue.baseapplication.helpers.EDGE_REWARD_DAY
import com.merryblue.baseapplication.helpers.EDGE_TRENDING
import com.merryblue.baseapplication.helpers.KEY_RECEIVE_DATA
import com.merryblue.baseapplication.helpers.RIPPLE_MAGICAL_BORDERS
import com.merryblue.baseapplication.helpers.RIPPLE_NATURE_SPAZ
import com.merryblue.baseapplication.helpers.RIPPLE_PREMIUM
import com.merryblue.baseapplication.helpers.TYPE_PRESET
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseActivity

@AndroidEntryPoint
class ThemesActivity : BaseActivity<ActivityThemesBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_themes

    private var initType: String? = TYPE_PRESET

    private lateinit var mediator: TabLayoutMediator

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
    }

    override fun setUpViews() {
        initType = intent.getStringExtra(KEY_RECEIVE_DATA)
        initTabLayout()
    }

    private fun initTabLayout() = binding.apply {
        tvTitleTheme.text = getString(if (initType == TYPE_PRESET) R.string.txt_preset else R.string.txt_themes)

        val titles = when (initType) {
            TYPE_PRESET -> listOf(
                EDGE_FIM to getString(R.string.txt_static),
                EDGE_MOST to getString(R.string.txt_most_downloaded),
                EDGE_REWARD_DAY to getString(R.string.txt_daily_rewards),
                EDGE_TRENDING to getString(R.string.txt_trending_today)
            )
            else -> listOf(
                RIPPLE_MAGICAL_BORDERS to getString(R.string.txt_magical_borders),
                RIPPLE_PREMIUM to getString(R.string.txt_premium)
            )
        }

        vpThemes.adapter = ThemePagerAdapter(this@ThemesActivity, titles)
        mediator = TabLayoutMediator(tabTheme, vpThemes) { tab, position ->
            tab.text = titles[position].second
        }.apply { attach() }

        btnBackTheme.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediator.isInitialized) mediator.detach()
    }
}