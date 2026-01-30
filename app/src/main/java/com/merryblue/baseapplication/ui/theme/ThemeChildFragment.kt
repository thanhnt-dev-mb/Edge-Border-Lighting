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
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.FragmentThemeChildBinding
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.ThemeUi
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
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.picker.ColorPickerActivity
import com.merryblue.baseapplication.ui.wallpaper.EdgeWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.RippleWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.StaticWallpaperSettingsActivity
import com.merryblue.baseapplication.ui.wallpaper.VideoWallpaperSettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment

@AndroidEntryPoint
class ThemeChildFragment : BaseFragment<FragmentThemeChildBinding>() {

    private val viewModel: ThemeViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val prefs by lazy { AppPreferences(requireContext()) }
    private var typeTheme: String = RIPPLE_MAGICAL_BORDERS
    private var isGallery: Boolean = false
    private var isCustom: Boolean = false
    private var currentType = WallpaperType.TYPE_STATIC

    private val onClick: (Item) -> Unit = { handleItemClick(it) }
    private val onGalleryClick: (ThemeUi.Gallery) -> Unit = {
        openGalleryPickOne()
    }

    private val onThemeCustomClick: (ThemeUi.Custom) -> Unit = {
        handelThemeCustomClick()
    }

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { handlePickedImage(it) }
    }

    private val pickFromGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            handlePickedImage(uri)
        }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handlePickedImage(it) }
    }

    private val themeAdapter by lazy {
        ThemeChildAdapter(onClick, onGalleryClick, onThemeCustomClick)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            typeTheme = it.getString(TYPE_THEME, RIPPLE_MAGICAL_BORDERS)
            isGallery = it.getBoolean(KEY_IS_GALLERY, false)
            isCustom = it.getBoolean(KEY_IS_CUSTOM, false)
        }
    }

    override fun getLayoutId(): Int = R.layout.fragment_theme_child

    override fun setUpViews() {
        binding.rcvTheme.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = themeAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.getPaging(typeTheme, isGallery, isCustom).collectLatest { themeAdapter.submitData(it) }
                }

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
                        val key = pair.first
                        val bmp = pair.second
                        bmp?.let {

                            val canSetLive = prefs.canChangeLive || prefs.canLiveChooser

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
                        } ?: run { Toast.makeText(requireContext(), getString(R.string.an_error_has_occurred), Toast.LENGTH_SHORT).show() }
                        AppLoading.closeLoading()
                    }
                }
            }
        }
    }

    private fun openGalleryPickOne() {
        if (!isAdded || isDetached) return
        if (!viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            return
        }

        val pickIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }

        val pm = requireContext().packageManager
        val canPick = pickIntent.resolveActivity(pm) != null

        if (canPick) {
            pickFromGallery.launch(Intent.createChooser(pickIntent, getString(R.string.app_name)))
            return
        }

        try {
            getContent.launch("image/*")
        } catch (_: Throwable) {
            Toast.makeText(requireContext(), "Cannot open gallery on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePickedImage(uri: Uri) {
        homeViewModel.loadBackgroundUri(uri, requireContext().getFullScreenTargetSize())
    }

    private fun handelThemeCustomClick() {
        homeViewModel.saveCacheEdgeState()
        startActivity(Intent(requireContext(), ColorPickerActivity::class.java))
    }

    private fun handleItemClick(item: Item) {
        AppLoading.displayLoading(requireContext())

        currentType = item.type

        val canSetLive = prefs.canChangeLive || prefs.canLiveChooser

        if (canSetLive) {
            when (item.type) {
                WallpaperType.TYPE_EDGE -> homeViewModel.loadEdgeBackgroundUrl(item, requireContext().getFullScreenTargetSize())

                WallpaperType.TYPE_STATIC -> homeViewModel.loadStaticBackgroundUrl(item, requireContext().getFullScreenTargetSize())

                WallpaperType.TYPE_VIDEO -> {
                    homeViewModel.videoUrl = item.pathUrl
                    homeViewModel.sendActionBroadcast(ACTION_EDGE_OVERLAY_STOP)
                    startActivity(Intent(requireContext(), VideoWallpaperSettingsActivity::class.java))
                    VideoPreloader.preload(requireContext().applicationContext, item.pathUrl) {
                        AppLoading.closeLoading()
                    }
                }

                WallpaperType.TYPE_RIPPLE -> {
                    homeViewModel.rippleEffectUrl = item.pathUrl
                    homeViewModel.loadBackgroundRippleUrl(item, requireContext().getFullScreenTargetSize())
                }
            }

        } else homeViewModel.loadStaticBackgroundUrl(item, requireContext().getFullScreenTargetSize())
    }

    companion object {
        @JvmStatic
        fun newInstance(theme: String, isGallery: Boolean, isCustom: Boolean) = ThemeChildFragment().apply {
            arguments = Bundle().apply {
                putString(TYPE_THEME, theme)
                putBoolean(KEY_IS_GALLERY, isGallery)
                putBoolean(KEY_IS_CUSTOM, isCustom)
            }
        }
    }
}
