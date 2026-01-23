package com.merryblue.baseapplication.ui.theme

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.FragmentThemeChildBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.helpers.BitmapMemoryCache
import com.merryblue.baseapplication.helpers.PreviewType.KEY_EDGE
import com.merryblue.baseapplication.helpers.RIPPLE_MAGICAL_BORDERS
import com.merryblue.baseapplication.helpers.TYPE_THEME
import com.merryblue.baseapplication.helpers.VideoPreloader
import com.merryblue.baseapplication.helpers.WallpaperType
import com.merryblue.baseapplication.helpers.getFullScreenTargetSize
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.wallpaper.EdgeWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.RippleWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.VideoWallpaperSettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment

@AndroidEntryPoint
class ThemeChildFragment: BaseFragment<FragmentThemeChildBinding>() {

    private val viewModel: ThemeViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private var typeTheme: String = RIPPLE_MAGICAL_BORDERS
    private val onClick : (Item) -> Unit = { handleItemClick(it) }
    private val themeAdapter by lazy { ThemeChildAdapter(onClick) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { typeTheme = it.getString(TYPE_THEME, RIPPLE_MAGICAL_BORDERS) }
    }

    override fun getLayoutId(): Int = R.layout.fragment_theme_child

    override fun setUpViews() {
        val gridManager = GridLayoutManager(requireContext(), 3)
        binding.rcvTheme.apply {
            layoutManager = gridManager
            adapter = themeAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getPaging(typeTheme).collectLatest {
                        themeAdapter.submitData(it)
                    }
                }

                launch {
                    themeAdapter.loadStateFlow.collectLatest { loadState ->
                        val isListEmpty = themeAdapter.itemCount == 0
                        binding.progressBarTheme.isVisible = loadState.refresh is LoadState.Loading && isListEmpty
                        binding.tvEmptyTheme.isVisible = loadState.refresh is LoadState.NotLoading && isListEmpty
                        val errorState = loadState.refresh as? LoadState.Error
                        if (errorState != null) {
                            binding.tvEmptyTheme.text = String.format("%s", "Error: ${errorState.error.message}")
                            binding.tvEmptyTheme.isVisible = true
                            binding.progressBarTheme.isVisible = false
                        }
                    }
                }

                launch {
                    homeViewModel.bgBitmap.collectLatest { bmp ->
                        bmp?.let {
                            viewModel.updateEdgeState { state -> state.copy(isEnableEdgeLighting = false) }
                            BitmapMemoryCache.put(KEY_EDGE, it)
                            val intent = Intent(requireContext(), EdgeWallpaperSettingsActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(theme: String) = ThemeChildFragment().apply {
            arguments = Bundle().apply {
                putString(TYPE_THEME, theme)
            }
        }
    }

    private fun handleItemClick(item: Item) {
        when (item.type) {
            WallpaperType.TYPE_EDGE,
            WallpaperType.TYPE_STATIC -> {
                homeViewModel.onClickBackgroundUrl(item, requireContext().getFullScreenTargetSize())
            }

            WallpaperType.TYPE_VIDEO -> {
                homeViewModel.updateEdgeState { state -> state.copy(isEnableEdgeLighting = false) }
                homeViewModel.saveCacheEdgeState()
                homeViewModel.videoUrl = item.pathUrl
                startActivity(Intent(requireContext(), VideoWallpaperSettingsActivity::class.java))
                VideoPreloader.preload(requireContext().applicationContext, item.pathUrl)
            }

            WallpaperType.TYPE_RIPPLE -> {
                homeViewModel.rippleEffectUrl = item.pathUrl
                startActivity(Intent(requireContext(), RippleWallpaperSettingsActivity::class.java))
            }
        }

    }
}