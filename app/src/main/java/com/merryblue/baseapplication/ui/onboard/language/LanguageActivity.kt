package com.merryblue.baseapplication.ui.onboard.language

import android.content.Context
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityLanguageBinding
import com.merryblue.baseapplication.ui.onboard.intro.IntroActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.ads.openads.AdapterOpenAppManager
import org.app.core.base.BaseActivity
import org.app.core.base.binding.setOnSingleClickListener
import org.app.core.base.extensions.disable
import org.app.core.base.extensions.enable
import org.app.core.base.extensions.getColorR
import org.app.core.base.extensions.hide
import org.app.core.base.extensions.openActivityAndClearStack
import org.app.core.base.extensions.show
import org.app.core.base.utils.StringResId

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {

    override fun getLayoutId() = R.layout.activity_language

    private var _from: String = "onboard"
    private val viewModel: LanguageViewModel by viewModels()
    private lateinit var adapter: LanguageAdapter

    override fun setUpViews() {
        enableEdgeToEdge(binding.main, true)

        binding.viewModel = viewModel

        if (viewModel.isFirstTime() && !viewModel.onboardedLanguage && !viewModel.isPremium()) {
            adsContainer = binding.adsContainer
            layoutCard = binding.layoutCard
        } else {
            binding.layoutCard.hide()
        }

        supportActionBar?.hide()
        AdapterOpenAppManager.instance.registerDisableOpenAdsAt(LanguageActivity::class.java)

        intent.extras?.getString(KEY_FROM)?.let { from ->
            this._from = from
        }
        binding.nextBtn.disable()
        if (this._from == "onboard") {
            binding.backBtn.hide()
        } else {
            binding.backBtn.show()
            binding.nextBtn.text = getString(StringResId.done)
        }

        binding.backBtn.setOnSingleClickListener {
            finish()
        }

        binding.nextBtn.setOnSingleClickListener {
            binding.topHandView.hide()
            viewModel.updateUserLanguage()
            val language = viewModel.selectedLanguage
            language?.let {
                updateLocale(language.value)
            }
            if (this._from == "onboard") {
                openActivityAndClearStack(IntroActivity::class.java)
            } else {
                finish()
            }
        }

        initRecyclerView()
        if (this._from == "onboard") {
            setupLottieView()
        }

        super.setUpViews()
    }

    override fun setUpObserver() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.connectionState.collectLatest { connected ->
                    onNetworkStateChanged(connected)
                }
            }
        }
    }

    private fun initRecyclerView() {
        val aBinding = ensureBindingNotNull

        aBinding?.let {
            val recyclerView = binding.languageRv

            val languages = viewModel.getLanguage(this, this._from == "onboard")
            adapter = LanguageAdapter(languages,
                itemClick = { item ->
                    viewModel.selectedLanguage = item
                    binding.nextBtn.enable()
                    if (this._from == "onboard") {
                        setupTopHandView()
                    }
                })

            val mLayoutManage = LinearLayoutManager(this)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = mLayoutManage
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        binding.handleView.hide()
                    }
                }
            })
        } ?: kotlin.run {
            if (this._from == "onboard") {
                openActivityAndClearStack(IntroActivity::class.java)
            } else {
                finish()
            }
        }
    }

    private fun setupLottieView() {
        val primaryColor = getColorR(R.color.colorPrimary)
        binding.handleView.setAnimation(R.raw.ani_hand_click)
        binding.handleView.addValueCallback(
            KeyPath("**"), // "**" applies to all layers
            LottieProperty.COLOR
        ) { primaryColor }
        binding.handleView.addValueCallback(
            KeyPath("**"), // "**" applies to all layers
            LottieProperty.STROKE_COLOR
        ) { primaryColor }
        binding.handleView.playAnimation()
        binding.handleView.show()
    }

    private fun setupTopHandView() {
        binding.handleView.hide()
        val primaryColor = getColorR(R.color.colorPrimary)
        binding.topHandView.setAnimation(R.raw.ani_hand_click)
        binding.topHandView.addValueCallback(
            KeyPath("**"), // "**" applies to all layers
            LottieProperty.COLOR
        ) { primaryColor }
        binding.topHandView.addValueCallback(
            KeyPath("**"), // "**" applies to all layers
            LottieProperty.STROKE_COLOR
        ) { primaryColor }
        binding.topHandView.playAnimation()
        binding.topHandView.show()
    }


    companion object {
        const val KEY_FROM = "key_start_from"

        fun open(context: Context, from: String = "onboard") {
            val intent = Intent(context, LanguageActivity::class.java)
            intent.putExtra(KEY_FROM, from)
            if (from == "onboard") {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }
}