package com.merryblue.baseapplication.ui.theme

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.merryblue.baseapplication.domain.model.ThemeUi
import com.merryblue.baseapplication.helpers.KEY_IS_ALL
import com.merryblue.baseapplication.helpers.KEY_IS_CUSTOM
import com.merryblue.baseapplication.helpers.PreviewType.KEY_EDGE
import com.merryblue.baseapplication.helpers.PreviewType.KEY_RIPPLE
import com.merryblue.baseapplication.helpers.RIPPLE_MAGICAL_BORDERS
import com.merryblue.baseapplication.helpers.TYPE_THEME
import com.merryblue.baseapplication.helpers.WallpaperType
import com.merryblue.baseapplication.helpers.cache.WallpaperBgStore
import com.merryblue.baseapplication.helpers.getFullScreenTargetSize
import com.merryblue.baseapplication.helpers.video.VideoPreloader
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.picker.ColorPickerActivity
import com.merryblue.baseapplication.ui.wallpaper.EdgeWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.RippleWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.VideoWallpaperSettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment
import timber.log.Timber

@AndroidEntryPoint
class ThemeChildFragment: BaseFragment<FragmentThemeChildBinding>() {

    private val viewModel: ThemeViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private var typeTheme: String = RIPPLE_MAGICAL_BORDERS
    private var isAllTheme: Boolean = false
    private var isCustom: Boolean = false
    private val onClick : (Item) -> Unit = { handleItemClick(it) }
    private val onGalleryClick: (ThemeUi.Gallery) -> Unit = {
        openGalleryPickOne()
    }

    private val onThemeCustomClick: (ThemeUi.Custom) -> Unit = {
        handelThemeCustomClick()
    }

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { handlePickedImage(it) }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handlePickedImage(it) }
    }

    private val themeAdapter by lazy { ThemeChildAdapter(onClick, onGalleryClick, onThemeCustomClick) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { typeTheme = it.getString(TYPE_THEME, RIPPLE_MAGICAL_BORDERS) }
        arguments?.let { isAllTheme = it.getBoolean(KEY_IS_ALL, false) }
        arguments?.let { isCustom = it.getBoolean(KEY_IS_CUSTOM, false) }
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
                    viewModel.getPaging(typeTheme, isAllTheme, isCustom).collectLatest {
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
                    homeViewModel.bgBitmap.collectLatest { pair ->
                        val key = pair.first
                        val bmp = pair.second
                        bmp?.let {
                            when (key) {
                                KEY_EDGE -> {
                                    viewModel.updateEdgeState { state -> state.copy(isEnableEdgeLighting = false) }
                                    WallpaperBgStore.saveFile(requireContext(), it)
                                    startActivity(Intent(requireContext(), EdgeWallpaperSettingsActivity::class.java))
                                }

                                KEY_RIPPLE -> {
                                    WallpaperBgStore.saveRippleAndNotify(requireContext(), it)
                                    startActivity(Intent(requireContext(), RippleWallpaperSettingsActivity::class.java))
                                }
                            }
                        } ?: run {
                            Toast.makeText(requireContext(), getString(R.string.an_error_has_occurred), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun openGalleryPickOne() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(requireContext())) {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            getContent.launch("image/*")
        }
    }

    private fun handlePickedImage(uri: Uri) {
        homeViewModel.onClickBackgroundUri(uri, requireContext().getFullScreenTargetSize())
    }

    private fun handelThemeCustomClick() {
        homeViewModel.saveCacheEdgeState()
        startActivity(Intent(requireContext(), ColorPickerActivity::class.java))
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
                homeViewModel.loadBackgroundRippleUrl(item, requireContext().getFullScreenTargetSize())
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(theme: String, isAll: Boolean, isCustom: Boolean) = ThemeChildFragment().apply {
            arguments = Bundle().apply {
                putString(TYPE_THEME, theme)
                putBoolean(KEY_IS_ALL, isAll)
                putBoolean(KEY_IS_CUSTOM, isCustom)
            }
        }
    }
}