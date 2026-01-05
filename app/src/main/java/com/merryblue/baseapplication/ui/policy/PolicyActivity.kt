package com.merryblue.baseapplication.ui.policy

import androidx.activity.viewModels
import androidx.core.graphics.drawable.toDrawable
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityPolicyBinding
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.getColorR
import org.app.core.base.extensions.hide
import org.app.core.base.utils.StringResId

@AndroidEntryPoint
class PolicyActivity : BaseActivity<ActivityPolicyBinding>() {
    private val viewModel: PolicyViewModel by viewModels()
    
    override fun getLayoutId() = R.layout.activity_policy

    override fun setUpViews() {
        binding.appbar.hide()
        supportActionBar?.setBackgroundDrawable(getColorR(org.app.core.R.color.white).toDrawable())
        supportActionBar?.elevation = 0f
        supportActionBar?.title = getString(StringResId.privacy)
    }
}