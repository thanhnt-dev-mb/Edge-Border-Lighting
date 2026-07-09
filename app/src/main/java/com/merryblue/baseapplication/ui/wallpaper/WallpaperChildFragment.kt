package com.merryblue.baseapplication.ui.wallpaper

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.FragmentThemeChildBinding
import com.merryblue.baseapplication.databinding.FragmentWallpaperChildBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.ThemeUi
import com.merryblue.baseapplication.enums.InterstitialFunction
import com.merryblue.baseapplication.helpers.AppLoading
import com.merryblue.baseapplication.helpers.KEY_IS_CUSTOM
import com.merryblue.baseapplication.helpers.KEY_IS_GALLERY
import com.merryblue.baseapplication.helpers.PreviewType.EDGE_WALLPAPER_SCREEN
import com.merryblue.baseapplication.helpers.PreviewType.RIPPLE_WALLPAPER_SCREEN
import com.merryblue.baseapplication.helpers.PreviewType.STATIC_WALLPAPER_SCREEN
import com.merryblue.baseapplication.helpers.RIPPLE_MAGICAL_BORDERS
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_STOP
import com.merryblue.baseapplication.helpers.TYPE_THEME
import com.merryblue.baseapplication.helpers.WallpaperType
import com.merryblue.baseapplication.helpers.cache.WallpaperBgStore
import com.merryblue.baseapplication.helpers.getFullScreenTargetSize
import com.merryblue.baseapplication.helpers.video.VideoPreloader
import com.merryblue.baseapplication.ui.home.HomeActivity
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.picker.ColorPickerActivity
import com.merryblue.baseapplication.ui.theme.ThemeChildAdapter
import com.merryblue.baseapplication.ui.theme.ThemeViewModel
import com.merryblue.baseapplication.ui.theme.ThemesActivity
import com.merryblue.baseapplication.ui.wallpaper.EdgeWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.ParallaxWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.RippleWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.StaticWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.VideoWallpaperSettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment

@AndroidEntryPoint
class WallpaperChildFragment : BaseFragment<FragmentWallpaperChildBinding>() {

    private val viewModel: WallpaperViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private var typeTheme: String = RIPPLE_MAGICAL_BORDERS
    private var isGallery: Boolean = false
    private var isCustom: Boolean = false

    private val onClick: (Item) -> Unit = { handleItemClick(it) }
    private val onGalleryClick: (ThemeUi.Gallery) -> Unit = { openGalleryPickOneSafe() }
    private val onThemeCustomClick: (ThemeUi.Custom) -> Unit = { handelThemeCustomClick() }

    private val pickPhoto13Plus = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { handlePickedImage(it) }
    }

    private val getContentLegacy = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handlePickedImage(it) }
    }

    private val themeAdapter by lazy {
        ThemeChildAdapter(onClick, onGalleryClick, onThemeCustomClick)
    }
    private val parallaxThumbMotionScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            themeAdapter.setParallaxThumbMotionEnabled(newState == RecyclerView.SCROLL_STATE_IDLE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            typeTheme = it.getString(TYPE_THEME, RIPPLE_MAGICAL_BORDERS)
            isGallery = it.getBoolean(KEY_IS_GALLERY, false)
            isCustom = it.getBoolean(KEY_IS_CUSTOM, false)
        }
    }

    override fun getLayoutId(): Int = R.layout.fragment_wallpaper_child

    override fun setUpViews() {
        binding.rcvTheme.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = themeAdapter
            setHasFixedSize(true)
            itemAnimator = null
            addOnScrollListener(parallaxThumbMotionScrollListener)
        }
    }

    override fun setupObservers() {
        super.setupObservers()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getPaging(typeTheme, isGallery, isCustom).collectLatest { themeAdapter.submitData(it) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    themeAdapter.loadStateFlow.collectLatest { loadState ->
                        val isEmpty = themeAdapter.itemCount == 0
                        binding.progressBarTheme.isVisible = loadState.refresh is LoadState.Loading && isEmpty
                        binding.tvEmptyTheme.isVisible = loadState.refresh is LoadState.NotLoading && isEmpty

                        val error = loadState.refresh as? LoadState.Error
                        error?.let {
                            binding.tvEmptyTheme.text = String.format("%s", "Error: ${it.error.message}")
                            binding.tvEmptyTheme.isVisible = true
                            binding.progressBarTheme.isVisible = false
                        }
                    }
                }

                launch {
                    homeViewModel.bgBitmap.collectLatest { pair ->
                        AppLoading.closeLoading()

                        val key = pair.first
                        val bmp = pair.second
                        bmp?.let {
                            val canSetLive = viewModel.canSetLive()

                            if (canSetLive) {
                                when (key) {
                                    EDGE_WALLPAPER_SCREEN -> {
                                        WallpaperBgStore.saveFile(requireContext(), it)
                                        startActivity(Intent(requireContext(), EdgeWallpaperSettingsActivity::class.java))
                                    }

                                    RIPPLE_WALLPAPER_SCREEN -> {
                                        WallpaperBgStore.saveRippleAndNotify(requireContext(), it)
                                        startActivity(Intent(requireContext(), RippleWallpaperSettingsActivity::class.java))
                                    }

                                    STATIC_WALLPAPER_SCREEN -> {
                                        WallpaperBgStore.saveFile(requireContext(), it)
                                        startActivity(Intent(requireContext(), StaticWallpaperSettingsActivity::class.java))
                                    }
                                }
                            } else {
                                WallpaperBgStore.saveFile(requireContext(), it)
                                startActivity(Intent(requireContext(), StaticWallpaperSettingsActivity::class.java))
                            }
                        } ?: run {
                            Toast.makeText(requireContext(), getString(R.string.an_error_has_occurred), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun canLaunchPicker(): Boolean {
        if (!isAdded || isDetached) return false
        if (!isResumed) return false
        if (parentFragmentManager.isStateSaved) return false
        if (!viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return false
        return true
    }

    private fun openGalleryPickOneSafe() {
        if (!canLaunchPicker()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickPhoto13Plus.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            getContentLegacy.launch("image/*")
        }
    }

    private fun handlePickedImage(uri: Uri) {
        homeViewModel.loadBackgroundUri(uri, requireContext().getFullScreenTargetSize())
    }

    private fun handelThemeCustomClick() {
        startActivity(Intent(requireContext(), ColorPickerActivity::class.java))
    }

    private fun handleItemClick(item: Item) {
        if (activity is ThemesActivity) {
            (activity as ThemesActivity).handleItemClick(item)
        } else if (activity is HomeActivity) {
            (activity as HomeActivity).handleItemClick(item)
        }
    }

    override fun onDestroyView() {
        binding.rcvTheme.removeOnScrollListener(parallaxThumbMotionScrollListener)
        themeAdapter.setParallaxThumbMotionEnabled(false)
        binding.rcvTheme.adapter = null
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance(theme: String, isGallery: Boolean, isCustom: Boolean) =
            WallpaperChildFragment().apply {
                arguments = Bundle().apply {
                    putString(TYPE_THEME, theme)
                    putBoolean(KEY_IS_GALLERY, isGallery)
                    putBoolean(KEY_IS_CUSTOM, isCustom)
                }
            }
    }
}
