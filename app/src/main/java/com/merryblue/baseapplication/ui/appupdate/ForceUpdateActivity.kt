package com.merryblue.baseapplication.ui.appupdate

import android.util.Log
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityForceUpdateBinding
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.redirectToPlayStore

@AndroidEntryPoint
class ForceUpdateActivity : BaseActivity<ActivityForceUpdateBinding>() {

    override fun getLayoutId() = R.layout.activity_force_update

    override fun setUpViews() {
        binding.forceUpdateTitle.text = getString(R.string.app_name) + " need an update"
        binding.forceUpdateDesc.text = "To continue use this " + getString(R.string.app_name) + ", please download the latest version"
        binding.forceUpdateInstallBtn.setOnClickListener {
            Log.i(TAG, "setUpViews: Handle force update...")
            redirectToPlayStore(packageName)
            finish()
        }
    }
}