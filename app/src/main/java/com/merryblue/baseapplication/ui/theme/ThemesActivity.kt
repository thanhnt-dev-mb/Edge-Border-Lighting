package com.merryblue.baseapplication.ui.theme

import android.os.Bundle
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityThemesBinding
import org.app.core.base.BaseActivity

class ThemesActivity : BaseActivity<ActivityThemesBinding>() {
    override fun getLayoutId(): Int = R.layout.activity_themes

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
    }

    override fun setUpViews() {

    }

    override fun setUpObserver() {

    }

}