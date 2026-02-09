package com.merryblue.baseapplication.ui.wallpaper

import com.google.android.material.tabs.TabLayoutMediator
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.FragmentWallpaperBinding
import com.merryblue.baseapplication.helpers.EDGE_FIM
import com.merryblue.baseapplication.helpers.KEY_ALL
import com.merryblue.baseapplication.helpers.RIPPLE_ABSTRACT_ABSCT
import com.merryblue.baseapplication.helpers.RIPPLE_MAGICAL_BORDERS
import com.merryblue.baseapplication.helpers.RIPPLE_NATURE_SPAZ
import com.merryblue.baseapplication.helpers.RIPPLE_PREMIUM
import com.merryblue.baseapplication.helpers.RIPPLE_RIPPLE
import com.merryblue.baseapplication.helpers.RIPPLE_TOP_PICS
import com.merryblue.baseapplication.ui.theme.ThemePagerAdapter
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseFragment

@AndroidEntryPoint
class WallpaperFragment : BaseFragment<FragmentWallpaperBinding>() {

    private lateinit var mediator: TabLayoutMediator
    private val prefs by lazy { AppPreferences(requireContext()) }

    override fun getLayoutId(): Int = R.layout.fragment_wallpaper

    override fun setUpViews() {
        initTabLayout()
    }

    private fun initTabLayout() = binding.apply {
        val canSetLive = prefs.canChangeLive || prefs.canLiveChooser

        val titles = buildList {
            add(KEY_ALL to getString(R.string.txt_all))

            if (canSetLive) add(RIPPLE_MAGICAL_BORDERS to getString(R.string.txt_magical_borders))
            if (canSetLive) add(RIPPLE_PREMIUM to getString(R.string.txt_premium))

            add(RIPPLE_NATURE_SPAZ to getString(R.string.txt_nature))
            add(RIPPLE_ABSTRACT_ABSCT to getString(R.string.txt_abstract))

//            if (canSetLive) add(RIPPLE_TOP_PICS to getString(R.string.txt_top_pics)) else add(EDGE_FIM to getString(R.string.txt_top_pics))

            add(RIPPLE_RIPPLE to getString(R.string.txt_ripple))
        }

        vpWallpaper.adapter = ThemePagerAdapter(
            isCustom = false,
            isGallery = true,
            activity = requireActivity(),
            titles = titles
        )
        mediator = TabLayoutMediator(tabWallpaper, vpWallpaper) { tab, position ->
            tab.text = titles[position].second
        }.apply { attach() }

        vpWallpaper.offscreenPageLimit = 3
    }

    override fun onDestroyView() {
        if (::mediator.isInitialized) mediator.detach()
        super.onDestroyView()
    }
}