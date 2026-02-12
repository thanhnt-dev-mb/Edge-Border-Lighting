package com.merryblue.baseapplication.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.merryblue.baseapplication.ui.home.color.EdgeColorFragment
import com.merryblue.baseapplication.ui.home.advanced.EdgeAdvancedFragment
import com.merryblue.baseapplication.ui.home.effect.EdgeEffectFragment

class SettingEdgeAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> EdgeColorFragment()
            1 -> EdgeEffectFragment()
            else -> EdgeAdvancedFragment()
        }
    }

    override fun getItemCount(): Int = 3
}