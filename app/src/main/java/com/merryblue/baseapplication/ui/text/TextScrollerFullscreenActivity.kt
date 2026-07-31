package com.merryblue.baseapplication.ui.text

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityTextScrollerFullscreenBinding
import org.app.core.base.BaseActivity

class TextScrollerFullscreenActivity : BaseActivity<ActivityTextScrollerFullscreenBinding>() {

    override fun getLayoutId(): Int = R.layout.activity_text_scroller_fullscreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.applyFullscreen()
    }

    override fun setUpViews() {
        super.setUpViews()

        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty().ifBlank { getString(R.string.hello_world) }
        val textColor = intent.getIntExtra(EXTRA_TEXT_COLOR, 0xFFFFFFFF.toInt())
        val backgroundColor = intent.getIntExtra(EXTRA_BACKGROUND_COLOR, 0xFF000000.toInt())
        val fontPath = intent.getStringExtra(EXTRA_FONT_PATH).orEmpty()
        val scroller = intent.getBooleanExtra(EXTRA_SCROLLER, true)
        val blink = intent.getBooleanExtra(EXTRA_BLINK, false)

        binding.fullscreenPreview.apply {
            keepScreenOn = true
            setTextValue(text)
            setTextSizeSp(FULLSCREEN_TEXT_SIZE_SP)
            setTextColorInt(textColor)
            setPreviewBackgroundColor(backgroundColor)
            setPreviewTypeface(TextScrollerFontCache.get(this@TextScrollerFullscreenActivity, fontPath))
            setEffects(scroller = scroller, blink = blink)
            setScrollSpeedDpPerSecond(FULLSCREEN_SCROLL_SPEED_DP_PER_SECOND)
            setOnClickListener { finish() }
        }
    }

    @Suppress("DEPRECATION")
    private fun Window.applyFullscreen() {
        addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    companion object {
        private const val EXTRA_TEXT = "extra_text"
        private const val EXTRA_TEXT_COLOR = "extra_text_color"
        private const val EXTRA_BACKGROUND_COLOR = "extra_background_color"
        private const val EXTRA_FONT_PATH = "extra_font_path"
        private const val EXTRA_SCROLLER = "extra_scroller"
        private const val EXTRA_BLINK = "extra_blink"
        private const val FULLSCREEN_TEXT_SIZE_SP = 92f
        private const val FULLSCREEN_SCROLL_SPEED_DP_PER_SECOND = 130f

        fun open(
            context: Context,
            text: String,
            textColor: Int,
            backgroundColor: Int,
            fontPath: String,
            scroller: Boolean,
            blink: Boolean
        ) {
            val intent = Intent(context, TextScrollerFullscreenActivity::class.java).apply {
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_TEXT_COLOR, textColor)
                putExtra(EXTRA_BACKGROUND_COLOR, backgroundColor)
                putExtra(EXTRA_FONT_PATH, fontPath)
                putExtra(EXTRA_SCROLLER, scroller)
                putExtra(EXTRA_BLINK, blink)
            }
            context.startActivity(intent)
        }
    }
}
