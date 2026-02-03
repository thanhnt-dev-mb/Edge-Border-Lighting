package com.merryblue.baseapplication.ui.home.advanced

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.databinding.FragmentEdgeAdvancedBinding
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.helpers.mapFloatToRange
import com.merryblue.baseapplication.helpers.mapValueToProgress
import com.merryblue.baseapplication.ui.home.HomeFragment
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.view.CustomSeekBar
import com.merryblue.baseapplication.ui.view.CustomSeekBar.OnProgressChangeListener
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeHoleShape
import com.merryblue.baseapplication.ui.view.edgelight.model.InfinityShape
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment
import timber.log.Timber
import kotlin.getValue

@AndroidEntryPoint
class EdgeAdvancedFragment : BaseFragment<FragmentEdgeAdvancedBinding>() {

    private val viewModel: EdgeAdvancedViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var directionAdapter: EdgeDirectionAdapter
    private lateinit var notchTypeAdapter: EdgeNotchTypeAdapter

    private val tabButtonsHole by lazy {
        listOf(binding.layoutDisplayHole.btnCircleHole, binding.layoutDisplayHole.btnRoundHole)
    }
    private val checkIconsHole by lazy {
        listOf(binding.layoutDisplayHole.ivCircleHole, binding.layoutDisplayHole.ivRoundHole)
    }
    private val tabButtonsInfinity by lazy {
        listOf(binding.layoutDisplayInfinity.btnInfinityU, binding.layoutDisplayInfinity.btnInfinityV)
    }
    private val checkIconsInfinity by lazy {
        listOf(binding.layoutDisplayInfinity.ivInfinityU, binding.layoutDisplayInfinity.ivInfinityV)
    }

    override fun getLayoutId(): Int = R.layout.fragment_edge_advanced

    override fun initView(view: View) {
        setupRecyclerView()
        registerOnClick()
        applyInitialSlidersFromSaved()
        applyInitialTabsFromSaved()
        viewModel.dispatch(EdgeAdvancedIntent.LoadInitial)
    }

    private fun setupRecyclerView() = with(binding) {
        directionAdapter = EdgeDirectionAdapter {
            viewModel.dispatch(EdgeAdvancedIntent.SelectDirection(it))
        }
        notchTypeAdapter = EdgeNotchTypeAdapter {
            viewModel.dispatch(EdgeAdvancedIntent.SelectNotchType(it))
        }

        rcvDirection.apply {
            adapter = directionAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
            itemAnimator = null
            isNestedScrollingEnabled = false
        }

        rcvNotchType.apply {
            adapter = notchTypeAdapter
            layoutManager = GridLayoutManager(requireContext(), 4)
            setHasFixedSize(true)
            itemAnimator = null
            isNestedScrollingEnabled = false
        }
    }

    private fun applyInitialSlidersFromSaved() = with(binding) {
        val s = viewModel.getEdgeState()

        // NOTCH UI
        layoutDisplayNotch.apply {
            seekBarNotchWidth.progress(s.notchWidthFraction.mapValueToProgress(min = 0.25f, max = 0.65f))
            seekBarNotchHeight.progress(s.notchHeightPx.mapValueToProgress(min = 10f.dpToPx, max = 80f.dpToPx))
            seekBarNotchTopRadius.progress(s.notchTopRadiusPx.mapValueToProgress(min = 0f.dpToPx, max = 40f.dpToPx))
            seekBarNotchBottomRadius.progress(s.notchBottomRadiusPx.mapValueToProgress(min = 0f.dpToPx, max = 60f.dpToPx))
            seekBarNotchBottomFullness.progress(s.notchBottomFullness.mapValueToProgress(min = 0f, max = 1f))
        }

        // HOLE UI
        layoutDisplayHole.apply {
            seekBarHoleLeft.progress(s.holeOffsetX.mapValueToProgress(0f, 1f))
            seekBarHoleTop.progress(s.holeOffsetY.mapValueToProgress(0f, 1f))
            seekBarHoleRadius.progress(s.holeRadius.mapValueToProgress(min = 6f.dpToPx, max = 30f.dpToPx))
            seekBarHoleRoundWidth.progress(s.holeWidthPx.mapValueToProgress(min = 40f.dpToPx, max = 180f.dpToPx))
            seekBarHoleRoundHeight.progress(s.holeHeightPx.mapValueToProgress(min = 16f.dpToPx, max = 80f.dpToPx))
            seekBarHoleCorner.progress(s.holeCornerRadiusPx.mapValueToProgress(min = 0f.dpToPx, max = 40f.dpToPx))
        }

        // INFINITY UI
        layoutDisplayInfinity.apply {
            seekBarInfinityWidth.progress(s.infinityWidthPx.mapValueToProgress(min = 60f.dpToPx, max = 360f.dpToPx))
            seekBarInfinityHeight.progress(s.infinityHeightPx.mapValueToProgress(min = 0f.dpToPx, max = 140f.dpToPx))
            seekBarInfinityTop.progress(s.infinityRadiusTopPx.mapValueToProgress(min = 0f.dpToPx, max = 60f.dpToPx))
        }
    }

    private fun applyInitialTabsFromSaved() {
        val s = viewModel.getEdgeState()
        showNotchTypeUi(s.notchType)
    }

    private fun registerOnClick() = with(binding) {
        // NOTCH SEEKBARS
        layoutDisplayNotch.apply {
            seekBarNotchWidth.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    val fraction = progress.mapFloatToRange(min = 0.25f, max = 0.65f)
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateNotchWidth(fraction))
                }
            })

            seekBarNotchHeight.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    val heightPx = progress.mapFloatToRange(min = 10f.dpToPx, max = 80f.dpToPx)
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateNotchHeight(heightPx))
                }
            })

            seekBarNotchTopRadius.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    val topRadiusPx = progress.mapFloatToRange(min = 0f.dpToPx, max = 40f.dpToPx)
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateNotchTopRadius(topRadiusPx))
                }
            })

            seekBarNotchBottomRadius.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    val bottomRadiusPx = progress.mapFloatToRange(0f.dpToPx, 60f.dpToPx)
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateNotchBottomRadius(bottomRadiusPx))
                }
            })

            seekBarNotchBottomFullness.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateNotchBottomFullness(progress))
                }
            })
        }

        // HOLE TAB BUTTONS & SEEKBARS
        layoutDisplayHole.apply {
            btnCircleHole.setOnClickListener {
                homeViewModel.applySettingEdgeLighting()
                viewModel.dispatch(EdgeAdvancedIntent.SelectHoleShape(EdgeHoleShape.CIRCLE))
            }

            btnRoundHole.setOnClickListener {
                homeViewModel.applySettingEdgeLighting()
                viewModel.dispatch(EdgeAdvancedIntent.SelectHoleShape(EdgeHoleShape.ROUND))
            }

            seekBarHoleLeft.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateHoleOffsetX(progress))
                }
            })

            seekBarHoleTop.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateHoleOffsetY(progress))
                }
            })

            seekBarHoleRadius.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    val r = progress.mapFloatToRange(6f.dpToPx, 30f.dpToPx)
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateHoleRadius(r))
                }
            })

            seekBarHoleRoundWidth.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    val w = progress.mapFloatToRange(40f.dpToPx, 180f.dpToPx)
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateHoleWidth(w))
                }
            })

            seekBarHoleRoundHeight.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    val h = progress.mapFloatToRange(16f.dpToPx, 80f.dpToPx)
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateHoleHeight(h))
                }
            })

            seekBarHoleCorner.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    val cr = progress.mapFloatToRange(0f.dpToPx, 40f.dpToPx)
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateHoleCornerRadius(cr))
                }
            })
        }

        // INFINITY TAB BUTTONS & SEEKBARS
        layoutDisplayInfinity.apply {
            btnInfinityU.setOnClickListener {
                homeViewModel.applySettingEdgeLighting()
                viewModel.dispatch(EdgeAdvancedIntent.SelectInfinityShape(InfinityShape.U))
            }

            btnInfinityV.setOnClickListener {
                homeViewModel.applySettingEdgeLighting()
                viewModel.dispatch(EdgeAdvancedIntent.SelectInfinityShape(InfinityShape.V))
            }

            seekBarInfinityWidth.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    val value = progress.mapFloatToRange(60f.dpToPx, 360f.dpToPx)
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateInfinityWidth(value))
                }
            })

            seekBarInfinityHeight.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    val value = progress.mapFloatToRange(0f.dpToPx, 140f.dpToPx)
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateInfinityHeight(value))
                }
            })

            seekBarInfinityTop.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.applySettingEdgeLighting()
                    val value = progress.mapFloatToRange(0f.dpToPx, 60f.dpToPx)
                    viewModel.dispatch(EdgeAdvancedIntent.UpdateInfinityTopRadius(value))
                }
            })
        }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe state changes
                launch {
                    viewModel.state.collect { state ->
                        directionAdapter.submitList(state.listDirection)
                        notchTypeAdapter.submitList(state.listNotchType)

                        // Render UI based on current selections
                        showNotchTypeUi(state.currentNotchType)
                    }
                }

                // Observe hole shape changes separately
                launch {
                    viewModel.state
                        .map { it.currentHoleShape }
                        .distinctUntilChanged()
                        .collect { holeShape ->
                            renderHoleShapeUI(holeShape)
                        }
                }

                // Observe infinity shape changes separately
                launch {
                    viewModel.state
                        .map { it.currentInfinityShape }
                        .distinctUntilChanged()
                        .collect { infinityShape ->
                            renderInfinityShapeUI(infinityShape)
                        }
                }

                // Observe effects
                launch {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is EdgeAdvancedEffect.RefreshLayout -> requestUpdateLayout()
                        }
                    }
                }
            }
        }
    }

    private fun selectTab(
        tabButtons: List<View>,
        checkIcons: List<AppCompatImageView>,
        selectedBtn: View,
        selectedCheck: AppCompatImageView
    ) {
        tabButtons.forEach { it.setBackgroundResource(R.drawable.bg_tab_color_edge_unselected) }
        checkIcons.forEach { it.setImageResource(R.drawable.ic_check_unselected) }
        selectedBtn.setBackgroundResource(R.drawable.bg_tab_color_edge_selected)
        selectedCheck.setImageResource(R.drawable.ic_check_selected)
    }

    private fun renderInfinityShapeUI(infinityShape: InfinityShape) = binding.layoutDisplayInfinity.apply {
        if (infinityShape == InfinityShape.U) {
            selectTab(tabButtonsInfinity, checkIconsInfinity, btnInfinityU, ivInfinityU)
        } else {
            selectTab(tabButtonsInfinity, checkIconsInfinity, btnInfinityV, ivInfinityV)
        }
    }

    private fun renderHoleShapeUI(edgeHoleShape: EdgeHoleShape) = binding.layoutDisplayHole.apply {
        if (edgeHoleShape == EdgeHoleShape.CIRCLE) {
            seekBarHoleRadius.visibility = View.VISIBLE
            tvHoleHeight.visibility = View.GONE
            tvHoleWidth.visibility = View.GONE
            seekBarHoleCorner.visibility = View.GONE
            seekBarHoleRoundWidth.visibility = View.GONE
            seekBarHoleRoundHeight.visibility = View.GONE
            selectTab(tabButtonsHole, checkIconsHole, btnCircleHole, ivCircleHole)
        } else {
            tvHoleHeight.visibility = View.VISIBLE
            tvHoleWidth.visibility = View.VISIBLE
            seekBarHoleCorner.visibility = View.VISIBLE
            seekBarHoleRoundWidth.visibility = View.VISIBLE
            seekBarHoleRoundHeight.visibility = View.VISIBLE
            seekBarHoleRadius.visibility = View.GONE
            selectTab(tabButtonsHole, checkIconsHole, btnRoundHole, ivRoundHole)
        }
    }

    private fun showNotchTypeUi(type: Advanced) = with(binding) {
        when (type) {
            Advanced.NOTCH_DEFAULT -> {
                layoutDisplayNotch.root.visibility = View.GONE
                layoutDisplayHole.root.visibility = View.GONE
                layoutDisplayInfinity.root.visibility = View.GONE
            }

            Advanced.NOTCH_DISPLAY_NOTCH -> {
                layoutDisplayNotch.root.visibility = View.VISIBLE
                layoutDisplayHole.root.visibility = View.GONE
                layoutDisplayInfinity.root.visibility = View.GONE
            }

            Advanced.NOTCH_DISPLAY_HOLE -> {
                layoutDisplayNotch.root.visibility = View.GONE
                layoutDisplayHole.root.visibility = View.VISIBLE
                layoutDisplayInfinity.root.visibility = View.GONE
            }

            Advanced.NOTCH_DISPLAY_INFINITY -> {
                layoutDisplayNotch.root.visibility = View.GONE
                layoutDisplayHole.root.visibility = View.GONE
                layoutDisplayInfinity.root.visibility = View.VISIBLE
            }

            else -> Unit
        }
    }

    private fun requestUpdateLayout() {
        binding.root.post { (parentFragment as? HomeFragment)?.onChildContentExpanded() }
    }
}