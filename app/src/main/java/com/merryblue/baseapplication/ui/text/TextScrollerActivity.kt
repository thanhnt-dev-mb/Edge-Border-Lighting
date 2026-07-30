package com.merryblue.baseapplication.ui.text

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityTextScrollerBinding
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.ui.text.model.Background
import com.merryblue.baseapplication.ui.text.model.Text
import com.merryblue.baseapplication.ui.text.model.TextEffect
import com.merryblue.baseapplication.ui.text.model.TextScrollerEffect
import com.merryblue.baseapplication.ui.text.model.Themes
import com.merryblue.baseapplication.ui.text.view.TextScrollerPreviewView
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseActivity

@AndroidEntryPoint
class TextScrollerActivity : BaseActivity<ActivityTextScrollerBinding>() {

    private val textScrollerViewModel: TextScrollerViewModel by viewModels()
    private lateinit var optionAdapter: TextScrollerOptionAdapter

    private var currentTab = TextScrollerTab.THEMES
    private var selectedThemeId: Int? = null
    private var selectedTextId: Int? = null
    private var selectedBackgroundId: Int? = null
    private val enabledEffects = linkedSetOf(TextScrollerEffect.SCROLLER)

    private var previewText: String = ""
    private var previewTextColor: Int = 0
    private var previewBackgroundColor: Int = 0
    private var previewFontPath: String = ""

    override fun getLayoutId(): Int = R.layout.activity_text_scroller

    override fun setUpViews() {
        super.setUpViews()
        previewText = getString(R.string.hello_world)
        setupPreviewDefaults()
        setupActions()
        setupTabs()
        setupRecyclerView()
        applyInitialTheme()
        showTab(TextScrollerTab.THEMES)
    }

    private fun setupPreviewDefaults() {
        previewTextColor = ContextCompat.getColor(this, R.color.colorGray09)
        previewBackgroundColor = ContextCompat.getColor(this, R.color.colorWhite)
        previewFontPath = DEFAULT_FONT_PATH
        binding.previewText.setDrawBackground(false)
    }

    private fun setupActions() = with(binding) {
        btnBackText.setOnClickListener { finish() }
        btnPlayText.setOnClickListener { openFullscreenPreview() }
        btnFullscreenPreview.setOnClickListener { openFullscreenPreview() }
    }

    private fun setupTabs() = with(binding.tabTextScroller) {
        TextScrollerTab.entries.forEach { tab ->
            addTab(newTab().setCustomView(createTabView(tab)))
        }
        applyTabItemSpacing()
        addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                updateTabSelectedState()
                showTab(TextScrollerTab.entries[tab.position])
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                updateTabSelectedState()
            }

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) = Unit
        })
        updateTabSelectedState()
        post {
            applyTabItemSpacing()
            updateTabSelectedState()
        }
    }

    private fun createTabView(tab: TextScrollerTab): AppCompatTextView {
        return LayoutInflater.from(this)
            .inflate(R.layout.item_text_scroller_tab, binding.tabTextScroller, false)
            .let { it as AppCompatTextView }
            .apply {
                text = getString(tab.titleRes)
                isSelected = true
            }
    }

    private fun applyTabItemSpacing() {
        val tabStrip = binding.tabTextScroller.getChildAt(0) as? ViewGroup ?: return
        for (index in 0 until tabStrip.childCount) {
            val child = tabStrip.getChildAt(index)
            val layoutParams = child.layoutParams as? ViewGroup.MarginLayoutParams ?: continue
            layoutParams.marginStart = TAB_ITEM_MARGIN_HORIZONTAL_DP.dpToPx
            layoutParams.marginEnd = TAB_ITEM_MARGIN_HORIZONTAL_DP.dpToPx
            child.layoutParams = layoutParams
        }
    }

    private fun updateTabSelectedState() = with(binding.tabTextScroller) {
        for (index in 0 until tabCount) {
            getTabAt(index)?.customView?.isActivated = index == selectedTabPosition
        }
    }

    private fun setupRecyclerView() {
        optionAdapter = TextScrollerOptionAdapter(
            fontProvider = { fontPath -> getTypeface(fontPath) },
            onClick = ::onOptionClicked
        )
        binding.rcvEffect.adapter = optionAdapter
    }

    private fun applyInitialTheme() {
        val theme = textScrollerViewModel.getDataThemes().firstOrNull() ?: return
        applyTheme(theme)
        applyPreview()
    }

    private fun showTab(tab: TextScrollerTab) {
        currentTab = tab
        binding.rcvEffect.layoutManager = GridLayoutManager(this, tab.spanCount)
        optionAdapter.submitItems(tab.toOptions(), selectedKeysFor(tab))
    }

    private fun TextScrollerTab.toOptions(): List<TextScrollerOption> {
        return when (this) {
            TextScrollerTab.THEMES -> textScrollerViewModel.getDataThemes().map(TextScrollerOption::ThemeOption)
            TextScrollerTab.TEXT -> textScrollerViewModel.getDataText().map(TextScrollerOption::TextOption)
            TextScrollerTab.BACKGROUND -> textScrollerViewModel.getDataBackground().map(TextScrollerOption::BackgroundOption)
            TextScrollerTab.EFFECT -> listOf(TextScrollerOption.EffectGroupOption(textScrollerViewModel.getDataEffect()))
        }
    }

    private fun selectedKeysFor(tab: TextScrollerTab): Set<String> {
        return when (tab) {
            TextScrollerTab.THEMES -> selectedThemeId?.let { setOf("theme_$it") }.orEmpty()
            TextScrollerTab.TEXT -> selectedTextId?.let { setOf("text_$it") }.orEmpty()
            TextScrollerTab.BACKGROUND -> selectedBackgroundId?.let { setOf("background_$it") }.orEmpty()
            TextScrollerTab.EFFECT -> enabledEffects.map { "effect_${it.name}" }.toSet()
        }
    }

    private fun onOptionClicked(option: TextScrollerOption) {
        when (option) {
            is TextScrollerOption.ThemeOption -> applyTheme(option.item)
            is TextScrollerOption.TextOption -> applyText(option.item)
            is TextScrollerOption.BackgroundOption -> applyBackground(option.item)
            is TextScrollerOption.EffectOption -> applyEffect(option.item)
            is TextScrollerOption.EffectGroupOption -> return
        }
        applyPreview()
        optionAdapter.updateSelection(selectedKeysFor(currentTab))
    }

    private fun applyTheme(theme: Themes) {
        selectedThemeId = theme.id
        selectedTextId = null
        selectedBackgroundId = null
        previewTextColor = ContextCompat.getColor(this, theme.contentColor)
        previewBackgroundColor = ContextCompat.getColor(this, theme.background)
        previewFontPath = theme.font
    }

    private fun applyText(text: Text) {
        selectedThemeId = null
        selectedTextId = text.id
        previewFontPath = text.font
    }

    private fun applyBackground(background: Background) {
        selectedThemeId = null
        selectedBackgroundId = background.id
        previewBackgroundColor = ContextCompat.getColor(this, background.color)
    }

    private fun applyEffect(effect: TextEffect) {
        if (enabledEffects.contains(effect.type)) {
            enabledEffects.remove(effect.type)
        } else {
            enabledEffects.add(effect.type)
        }
    }

    private fun applyPreview() {
        binding.cardPreview.setCardBackgroundColor(previewBackgroundColor)
        binding.previewText.applyStyle(textSizeSp = PREVIEW_TEXT_SIZE_SP)
    }

    private fun TextScrollerPreviewView.applyStyle(textSizeSp: Float) {
        setTextValue(previewText)
        setTextSizeSp(textSizeSp)
        setPreviewBackgroundColor(previewBackgroundColor)
        setTextColorInt(previewTextColor)
        setPreviewTypeface(getTypeface(previewFontPath))
        setEffects(
            scroller = enabledEffects.contains(TextScrollerEffect.SCROLLER),
            blink = enabledEffects.contains(TextScrollerEffect.BLINK)
        )
    }

    private fun openFullscreenPreview() {
        TextScrollerFullscreenActivity.open(
            context = this,
            text = previewText,
            textColor = previewTextColor,
            backgroundColor = previewBackgroundColor,
            fontPath = previewFontPath,
            scroller = enabledEffects.contains(TextScrollerEffect.SCROLLER),
            blink = enabledEffects.contains(TextScrollerEffect.BLINK)
        )
    }

    private fun getTypeface(fontPath: String): Typeface {
        return TextScrollerFontCache.get(this, fontPath)
    }

    private enum class TextScrollerTab(val titleRes: Int, val spanCount: Int) {
        THEMES(R.string.txt_themes, 3),
        TEXT(R.string.txt_text, 3),
        BACKGROUND(R.string.txt_background, 5),
        EFFECT(R.string.txt_effect, 1)
    }

    companion object {
        private const val DEFAULT_FONT_PATH = "font/inter_black.ttf"
        private const val PREVIEW_TEXT_SIZE_SP = 60f
        private const val TAB_ITEM_MARGIN_HORIZONTAL_DP = 2
    }

}
