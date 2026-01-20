package com.merryblue.baseapplication.ui.theme

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ThemePagerAdapter(activity: FragmentActivity, titles: List<Pair<String, String>>): FragmentStateAdapter(activity) {

    private val fragments by lazy {
        titles.map {
            ThemeChildFragment.newInstance(it.first)
        }
    }

    override fun getItemCount() = fragments.size
    override fun createFragment(position: Int): Fragment = fragments[position]
}