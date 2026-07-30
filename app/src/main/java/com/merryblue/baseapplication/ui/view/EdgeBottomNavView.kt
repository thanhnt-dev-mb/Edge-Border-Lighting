package com.merryblue.baseapplication.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import com.merryblue.baseapplication.R

class EdgeBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayoutCompat(context, attrs) {

    enum class Tab { EDGE, WALLPAPER, TEXT_SCROLLER, SETTING }

    private data class TabViews(
        val root: LinearLayoutCompat,
        val icon: AppCompatImageView,
        val title: AppCompatTextView
    )

    private var edge: TabViews
    private var wallpaper: TabViews
    private var textScroller: TabViews
    private var setting: TabViews

    var onTabSelected: ((Tab) -> Unit)? = null
    var selectedTab: Tab = Tab.WALLPAPER
        private set

    @DrawableRes private var edgeIconSelected = R.drawable.ic_edge_selected
    @DrawableRes private var edgeIconUnselected = R.drawable.ic_edge_unselected

    @DrawableRes private var wallpaperIconSelected = R.drawable.ic_wallpaper_selected
    @DrawableRes private var wallpaperIconUnselected = R.drawable.ic_wallpaper_unselected

    @DrawableRes private var textScrollerIconSelected = R.drawable.ic_text_sccroller_selected
    @DrawableRes private var textScrollerIconUnselected = R.drawable.ic_text_sccroller_unselected

    @DrawableRes private var settingIconSelected = R.drawable.ic_setting_selected
    @DrawableRes private var settingIconUnselected = R.drawable.ic_setting_unselected

    @ColorInt private var colorSelected: Int = ContextCompat.getColor(context, R.color.colorViolet05)
    @ColorInt private var colorUnselected: Int = ContextCompat.getColor(context, R.color.colorGray04)

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.layout_bottom_navigation, this, true)

        edge = bindTab(
            rootId = R.id.btnEdge,
            iconId = R.id.ivEdge,
            textId = R.id.tvEdge
        )
        wallpaper = bindTab(
            rootId = R.id.btnWallpaper,
            iconId = R.id.ivWallpaper,
            textId = R.id.tvWallpaper
        )
        textScroller = bindTab(
            rootId = R.id.btnTextScroller,
            iconId = R.id.ivTextScroller,
            textId = R.id.tvTextScroller
        )
        setting = bindTab(
            rootId = R.id.btnSetting,
            iconId = R.id.ivSetting,
            textId = R.id.tvSetting
        )

        edge.root.setOnClickListener { setSelectedTab(Tab.EDGE, fromUser = true) }
        wallpaper.root.setOnClickListener { setSelectedTab(Tab.WALLPAPER, fromUser = true) }
        textScroller.root.setOnClickListener { setSelectedTab(Tab.TEXT_SCROLLER, fromUser = true) }
        setting.root.setOnClickListener { setSelectedTab(Tab.SETTING, fromUser = true) }

        attrs?.let { parseAttrs(it) }

        setSelectedTab(selectedTab, fromUser = false)
    }

    private fun bindTab(rootId: Int, iconId: Int, textId: Int): TabViews {
        val root = findViewById<LinearLayoutCompat>(rootId)
        val icon = findViewById<AppCompatImageView>(iconId)
        val title = findViewById<AppCompatTextView>(textId)
        return TabViews(root, icon, title)
    }

    private fun parseAttrs(attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.EdgeBottomNavView)
        try {
            selectedTab = when (a.getInt(R.styleable.EdgeBottomNavView_defaultTab, 0)) {
                0 -> Tab.EDGE
                1 -> Tab.WALLPAPER
                2 -> Tab.TEXT_SCROLLER
                3 -> Tab.SETTING
                else -> Tab.EDGE
            }

            colorSelected = a.getColor(R.styleable.EdgeBottomNavView_tabColorSelected, colorSelected)
            colorUnselected = a.getColor(R.styleable.EdgeBottomNavView_tabColorUnselected, colorUnselected)

            edgeIconSelected = a.getResourceId(R.styleable.EdgeBottomNavView_edgeIconSelected, edgeIconSelected)
            edgeIconUnselected = a.getResourceId(R.styleable.EdgeBottomNavView_edgeIconUnselected, edgeIconUnselected)

            wallpaperIconSelected = a.getResourceId(R.styleable.EdgeBottomNavView_wallpaperIconSelected, wallpaperIconSelected)
            wallpaperIconUnselected = a.getResourceId(R.styleable.EdgeBottomNavView_wallpaperIconUnselected, wallpaperIconUnselected)

            textScrollerIconSelected = a.getResourceId(R.styleable.EdgeBottomNavView_textScrollerIconSelected, textScrollerIconSelected)
            textScrollerIconUnselected = a.getResourceId(R.styleable.EdgeBottomNavView_textScrollerIconUnselected, textScrollerIconUnselected)

            settingIconSelected = a.getResourceId(R.styleable.EdgeBottomNavView_settingIconSelected, settingIconSelected)
            settingIconUnselected = a.getResourceId(R.styleable.EdgeBottomNavView_settingIconUnselected, settingIconUnselected)
        } finally {
            a.recycle()
        }
    }

    fun setSelectedTab(tab: Tab, fromUser: Boolean = false) {
        if (selectedTab == tab) return
        selectedTab = tab
        render()
        if (fromUser) onTabSelected?.invoke(tab)
    }

    fun setOnTabSelectedListener(listener: (Tab) -> Unit) {
        onTabSelected = listener
    }

    private fun render() {
        applyState(edge, isSelected = selectedTab == Tab.EDGE, edgeIconSelected, edgeIconUnselected)
        applyState(wallpaper, isSelected = selectedTab == Tab.WALLPAPER, wallpaperIconSelected, wallpaperIconUnselected)
        applyState(textScroller, isSelected = selectedTab == Tab.TEXT_SCROLLER, textScrollerIconSelected, textScrollerIconUnselected)
        applyState(setting, isSelected = selectedTab == Tab.SETTING, settingIconSelected, settingIconUnselected)
    }

    private fun applyState(
        tab: TabViews,
        isSelected: Boolean,
        @DrawableRes iconSel: Int,
        @DrawableRes iconUnselected: Int
    ) {
        tab.icon.setImageResource(if (isSelected) iconSel else iconUnselected)
        tab.title.setTextColor(if (isSelected) colorSelected else colorUnselected)
        tab.root.isSelected = isSelected
    }

    fun setTabColors(@ColorInt selected: Int, @ColorInt unselected: Int) {
        colorSelected = selected
        colorUnselected = unselected
        render()
    }
}