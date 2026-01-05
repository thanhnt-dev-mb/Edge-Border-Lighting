package com.merryblue.baseapplication.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.BuildConfig
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityHomeBinding
import com.merryblue.baseapplication.helpers.Compatibility
import com.merryblue.baseapplication.helpers.isAppInstalled
import com.merryblue.baseapplication.helpers.isBackground
import com.merryblue.baseapplication.helpers.openPolicy
import com.merryblue.baseapplication.ui.appupdate.ForceUpdateActivity
import com.merryblue.baseapplication.ui.onboard.language.LanguageActivity
import com.merryblue.baseapplication.ui.widget.BottomSheetRate
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.ads.remoteconfig.CoreRemoteConfig
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.openActivityAndClearStack
import org.app.core.base.utils.StringResId


@AndroidEntryPoint
class HomeActivity : BaseActivity<ActivityHomeBinding>() {
    private val viewModel: HomeViewModel by viewModels()

    private var isFirstVisible = true
    private var showingRate: Boolean = false
    private var isActive: Boolean = false

    override
    fun getLayoutId() = R.layout.activity_home

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        requestPostNotificationPermissionIfNeed()
    }

    override fun setUpViews() {
        enableEdgeToEdge(binding.main, false)
        super.setUpViews()
    }

    override fun onResume() {
        super.onResume()

//        handleForceUpdateIfNeed()
        isFirstVisible = false
        isActive = true
    }
    
    override fun onPause() {
        super.onPause()
        isActive = false
    }
    
    private fun handleForceUpdateIfNeed() : Boolean {
        val isForceUpdate = CoreRemoteConfig.instance.checkForceUpdateIfNeed(BuildConfig.VERSION_CODE)
        if (isForceUpdate) {
            Handler(Looper.getMainLooper()).postDelayed({
                openActivityAndClearStack(ForceUpdateActivity::class.java)
            }, 300)
        }
        return isForceUpdate
    }

    fun handleShowReviewIfNeed() {
        if (!isActive || isBackground() || showingRate) return
    
        try {
            showingRate = true
            (supportFragmentManager.findFragmentByTag(BottomSheetRate.TAG) as? BottomSheetDialogFragment)?.dismissAllowingStateLoss()
            val bottom = BottomSheetRate {

            }
            bottom.show(supportFragmentManager, BottomSheetRate.TAG)
        } catch (ex: Exception) {
            showingRate = false
            ex.printStackTrace()
        }
    }

    private fun handleDrawerMenuAction(itemId: Int) {
        when(itemId) {
            R.id.language -> {
                LanguageActivity.open(this, "setting")
            }
            R.id.rate -> {
                handleShowReviewIfNeed()
            }
            R.id.feedback -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback.developer.app@gmail.com"))
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                val gmailPkg = "com.google.android.gm"
                val isGmailInstalled = isAppInstalled(gmailPkg)
                if (isGmailInstalled) {
                    intent.type = "text/html"
                    intent.setPackage(gmailPkg)
                    startActivity(intent);
                } else {
                    intent.type = "message/rfc822"
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        startActivity(Intent.createChooser(intent, "Choose an Email application to start"))
                    }
                }
                return
            }
            R.id.privacy -> {
                openPolicy()
            }
            else -> {}
        }
    }
    
    private fun requestPostNotificationPermissionIfNeed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (!Compatibility.hasPostNotificationsPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
            requestPermissions(arrayOf(
                Manifest.permission.POST_NOTIFICATIONS
            )) { isGranted: Boolean ->
                if (!isGranted) {
                    Toast.makeText(this, getString(StringResId.notificationOff), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
