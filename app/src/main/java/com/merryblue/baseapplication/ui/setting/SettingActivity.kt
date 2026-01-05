package com.merryblue.baseapplication.ui.setting

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivitySettingBinding
import com.merryblue.baseapplication.ui.iap.PurchaseActivity
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseActivity

@AndroidEntryPoint
class SettingActivity : BaseActivity<ActivitySettingBinding>() {
    private val viewModel: SettingViewModel by viewModels()
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun getLayoutId() = R.layout.activity_setting

    override fun setUpViews() {
        enableEdgeToEdge(binding.main, false)
        setSupportActionBar(binding.toolbar)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostContainerSetting) as? NavHostFragment
        navHostFragment?.navController?.let { navController ->
            appBarConfiguration = AppBarConfiguration(setOf())
            setupActionBarWithNavController(navController, appBarConfiguration)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostContainerSetting) as? NavHostFragment
        val navController = navHostFragment?.navController ?: return super.onSupportNavigateUp()
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object {

        fun open(context: Context) {
            val intent = Intent(context, SettingActivity::class.java)
            context.startActivity(intent)
        }
    }
}