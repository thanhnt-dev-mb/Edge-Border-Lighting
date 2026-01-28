package com.merryblue.baseapplication.ui.theme

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ThemePagerAdapter(isCustom: Boolean, isAllTheme: Boolean, activity: FragmentActivity, titles: List<Pair<String, String>>): FragmentStateAdapter(activity) {

    private val fragments by lazy {
        titles.mapIndexed { index, it ->
            ThemeChildFragment.newInstance(
                it.first,
                if (isAllTheme) index == 0 else false,
                if (isCustom) index == 0 else false
            )
        }
    }

    override fun getItemCount() = fragments.size
    override fun createFragment(position: Int): Fragment = fragments[position]
}