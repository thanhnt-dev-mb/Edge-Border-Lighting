package com.merryblue.baseapplication.ui.wallpaper

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.FragmentWallpaperBinding
import com.merryblue.baseapplication.helpers.KEY_ALL
import com.merryblue.baseapplication.helpers.PARALLAX_FOOTBALL
import com.merryblue.baseapplication.helpers.RIPPLE_ABSTRACT_ABSCT
import com.merryblue.baseapplication.helpers.RIPPLE_MAGICAL_BORDERS
import com.merryblue.baseapplication.helpers.RIPPLE_NATURE_SPAZ
import com.merryblue.baseapplication.helpers.RIPPLE_PREMIUM
import com.merryblue.baseapplication.helpers.RIPPLE_RIPPLE
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseFragment

@AndroidEntryPoint
class WallpaperFragment : BaseFragment<FragmentWallpaperBinding>() {
    private val viewModel: WallpaperViewModel by activityViewModels()

    private lateinit var mediator: TabLayoutMediator

    override fun getLayoutId(): Int = R.layout.fragment_wallpaper

    override fun setUpViews() {
        initTabLayout()
    }

    private fun initTabLayout() = binding.apply {
        val canSetLive = viewModel.canSetLive()

        val titles = buildList {
            add(KEY_ALL to getString(R.string.txt_all))

            if (canSetLive) add(PARALLAX_FOOTBALL to getString(R.string.txt_football))
            if (canSetLive) add(RIPPLE_MAGICAL_BORDERS to getString(R.string.txt_magical_borders))
            if (canSetLive) add(RIPPLE_PREMIUM to getString(R.string.txt_premium))

            add(RIPPLE_NATURE_SPAZ to getString(R.string.txt_nature))
            add(RIPPLE_ABSTRACT_ABSCT to getString(R.string.txt_abstract))

//            if (canSetLive) add(RIPPLE_TOP_PICS to getString(R.string.txt_top_pics)) else add(EDGE_FIM to getString(R.string.txt_top_pics))

            add(RIPPLE_RIPPLE to getString(R.string.txt_ripple))
        }

        vpWallpaper.adapter = WallpaperPagerAdapter(activity = requireActivity(), titles = titles)
        mediator = TabLayoutMediator(tabWallpaper, vpWallpaper) { tab, position ->
            tab.text = titles[position].second
        }.apply { attach() }

        vpWallpaper.offscreenPageLimit = 3
    }

    override fun onDestroyView() {
        if (::mediator.isInitialized) mediator.detach()
        super.onDestroyView()
    }

    class WallpaperPagerAdapter(activity: FragmentActivity, val titles: List<Pair<String, String>>): FragmentStateAdapter(activity) {
        private val fragments by lazy {
            titles.mapIndexed { index, it ->
                WallpaperChildFragment.newInstance(
                    it.first,
                    index == 0,
                    false
                )
            }
        }

        override fun getItemCount() = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}