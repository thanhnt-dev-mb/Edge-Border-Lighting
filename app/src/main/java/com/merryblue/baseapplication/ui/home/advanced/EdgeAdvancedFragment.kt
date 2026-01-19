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
import com.merryblue.baseapplication.coredata.model.edge.DisplayHole
import com.merryblue.baseapplication.coredata.model.edge.DisplayInfinity
import com.merryblue.baseapplication.coredata.model.edge.DisplayNotch
import com.merryblue.baseapplication.coredata.model.edge.DisplayNotchType
import com.merryblue.baseapplication.databinding.FragmentEdgeAdvancedBinding
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.helpers.mapFloatToRange
import com.merryblue.baseapplication.helpers.mapValueToProgress
import com.merryblue.baseapplication.ui.home.HomeFragment
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.view.CustomSeekBar
import com.merryblue.baseapplication.ui.view.CustomSeekBar.OnProgressChangeListener
import com.merryblue.baseapplication.ui.view.edgelight.EdgeHoleShape
import com.merryblue.baseapplication.ui.view.edgelight.InfinityShape
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment
import timber.log.Timber


@AndroidEntryPoint
class EdgeAdvancedFragment : BaseFragment<FragmentEdgeAdvancedBinding>() {

    private val viewModel: EdgeAdvancedViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var directionAdapter: EdgeDirectionAdapter
    private lateinit var notchTypeAdapter: EdgeNotchTypeAdapter
    private val tabButtonsHole by lazy { listOf(binding.layoutDisplayHole.btnCircleHole, binding.layoutDisplayHole.btnRoundHole) }
    private val checkIconsHole by lazy { listOf(binding.layoutDisplayHole.ivCircleHole, binding.layoutDisplayHole.ivRoundHole) }
    private val tabButtonsInfinity by lazy { listOf(binding.layoutDisplayInfinity.btnInfinityU, binding.layoutDisplayInfinity.btnInfinityV) }
    private val checkIconsInfinity by lazy { listOf(binding.layoutDisplayInfinity.ivInfinityU, binding.layoutDisplayInfinity.ivInfinityV) }

    override fun getLayoutId(): Int = R.layout.fragment_edge_advanced

    override fun initView(view: View) {
        setupRecyclerView()
        viewModel.loadInitialFromSaved()
        applyInitialSlidersFromSaved()
        applyInitialTabsFromSaved()
        registerOnClick()
    }

    private fun setupRecyclerView() = with(binding) {
        directionAdapter = EdgeDirectionAdapter { viewModel.selectDirection(it) }
        notchTypeAdapter = EdgeNotchTypeAdapter { viewModel.selectNotchType(it) }

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
        notchHoleUI(s.holeShape)
        notchInfinityUI(s.infinityShape)
    }

    private fun registerOnClick() = with(binding) {

        layoutDisplayNotch.apply {
            seekBarNotchWidth.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val fraction = progress.mapFloatToRange(min = 0.25f, max = 0.65f)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayNotch(progress = fraction, DisplayNotch.NOTCH_WIDTH))
                    viewModel.updateEdgeState { it.copy(notchWidthFraction = fraction) }
                }
            })

            seekBarNotchHeight.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val heightPx = progress.mapFloatToRange(min = 10f.dpToPx, max = 80f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayNotch(progress = heightPx, DisplayNotch.NOTCH_HEIGHT))
                    viewModel.updateEdgeState { it.copy(notchHeightPx = heightPx) }
                }
            })

            seekBarNotchTopRadius.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val topRadiusPx = progress.mapFloatToRange(min = 0f.dpToPx, max = 40f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayNotch(progress = topRadiusPx, DisplayNotch.NOTCH_TOP_RADIUS))
                    viewModel.updateEdgeState { it.copy(notchTopRadiusPx = topRadiusPx) }
                }
            })

            seekBarNotchBottomRadius.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val bottomRadiusPx = progress.mapFloatToRange(0f.dpToPx, 60f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayNotch(progress = bottomRadiusPx, DisplayNotch.NOTCH_BOTTOM_RADIUS))
                    viewModel.updateEdgeState { it.copy(notchBottomRadiusPx = bottomRadiusPx) }
                }
            })

            seekBarNotchBottomFullness.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayNotch(progress = progress, DisplayNotch.NOTCH_BOTTOM_FULLNESS))
                    viewModel.updateEdgeState { it.copy(notchBottomFullness = progress) }
                }
            })
        }

        layoutDisplayHole.apply {

            btnCircleHole.setOnClickListener {
                notchHoleUI(EdgeHoleShape.CIRCLE)
                homeViewModel.emitHoleType(EdgeHoleShape.CIRCLE)
                viewModel.updateEdgeState { it.copy(holeShape = EdgeHoleShape.CIRCLE) }
            }

            btnRoundHole.setOnClickListener {
                notchHoleUI(EdgeHoleShape.ROUND)
                homeViewModel.emitHoleType(EdgeHoleShape.ROUND)
                viewModel.updateEdgeState { it.copy(holeShape = EdgeHoleShape.ROUND) }
            }

            seekBarHoleLeft.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = progress, DisplayHole.HOLE_HORIZONTAL))
                    viewModel.updateEdgeState { it.copy(holeOffsetX = progress) }
                }
            })

            seekBarHoleTop.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = progress, DisplayHole.HOLE_VERTICAL))
                    viewModel.updateEdgeState { it.copy(holeOffsetY = progress) }
                }
            })

            seekBarHoleRadius.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val r = progress.mapFloatToRange(6f.dpToPx, 30f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = r, DisplayHole.HOLE_RADIUS))
                    viewModel.updateEdgeState { it.copy(holeRadius = r) }
                }
            })

            seekBarHoleRoundWidth.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val w = progress.mapFloatToRange(40f.dpToPx, 180f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = w, DisplayHole.HOLE_WIDTH))
                    viewModel.updateEdgeState { it.copy(holeWidthPx = w) }
                }
            })

            seekBarHoleRoundHeight.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val h = progress.mapFloatToRange(16f.dpToPx, 80f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = h, DisplayHole.HOLE_HEIGHT))
                    viewModel.updateEdgeState { it.copy(holeHeightPx = h) }
                }
            })

            seekBarHoleCorner.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val cr = progress.mapFloatToRange(0f.dpToPx, 40f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = cr, DisplayHole.HOLE_CORNER))
                    viewModel.updateEdgeState { it.copy(holeCornerRadiusPx = cr) }
                }
            })
        }

        layoutDisplayInfinity.apply {

            btnInfinityU.setOnClickListener {
                notchInfinityUI(InfinityShape.U)
                homeViewModel.emitInfinityType(InfinityShape.U)
                viewModel.updateEdgeState { it.copy(infinityShape = InfinityShape.U) }
            }

            btnInfinityV.setOnClickListener {
                notchInfinityUI(InfinityShape.V)
                homeViewModel.emitInfinityType(InfinityShape.V)
                viewModel.updateEdgeState { it.copy(infinityShape = InfinityShape.V) }
            }

            seekBarInfinityWidth.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val value = progress.mapFloatToRange(60f.dpToPx, 360f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayInfinity(progress = value, DisplayInfinity.INFINITY_WIDTH))
                    viewModel.updateEdgeState { it.copy(infinityWidthPx = value) }
                }
            })

            seekBarInfinityHeight.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val value = progress.mapFloatToRange(0f.dpToPx, 140f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayInfinity(progress = value, DisplayInfinity.INFINITY_HEIGHT))
                    viewModel.updateEdgeState { it.copy(infinityHeightPx = value) }
                }
            })

            seekBarInfinityTop.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val value = progress.mapFloatToRange(0f.dpToPx, 60f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayInfinity(progress = value, DisplayInfinity.INFINITY_TOP))
                    viewModel.updateEdgeState { it.copy(infinityRadiusTopPx = value) }
                }
            })
        }
    }

    private fun selectTab(tabButtons: List<View>, checkIcons: List<AppCompatImageView>, selectedBtn: View, selectedCheck: AppCompatImageView) {
        tabButtons.forEach { it.setBackgroundResource(R.drawable.bg_tab_color_edge_unselected) }
        checkIcons.forEach { it.setImageResource(R.drawable.ic_check_unselected) }
        selectedBtn.setBackgroundResource(R.drawable.bg_tab_color_edge_selected)
        selectedCheck.setImageResource(R.drawable.ic_check_selected)
    }

    private fun notchInfinityUI(infinityShape: InfinityShape) = binding.layoutDisplayInfinity.apply {
        if (infinityShape == InfinityShape.U) {
            selectTab(tabButtonsInfinity, checkIconsInfinity,btnInfinityU, ivInfinityU)
        } else {
            selectTab(tabButtonsInfinity, checkIconsInfinity,btnInfinityV, ivInfinityV)
        }
        requestUpdateLayout()
    }

    private fun notchHoleUI(edgeHoleShape: EdgeHoleShape) = binding.layoutDisplayHole.apply {
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
        requestUpdateLayout()
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.directionStateFlow.collect { state ->
                        val directionIndex = state.directionSelectedIndex
                        val directionItems = state.listDirection
                        directionAdapter.submitList(directionItems)
                        if (directionIndex in directionItems.indices) {
                            val selected = directionItems[directionIndex]
                            homeViewModel.emitEdgeAdvances(selected.type)
                            viewModel.updateEdgeState { it.copy(direction = selected.type) }
                        }
                    }
                }

                launch {
                    viewModel.notchTypeStateFlow.collect { state ->
                        val notchTypeIndex = state.notchTypeSelectedIndex
                        val notchTypeItems = state.listNotchType
                        notchTypeAdapter.submitList(notchTypeItems)
                        if (notchTypeIndex in notchTypeItems.indices) {
                            val selected = notchTypeItems[notchTypeIndex]
                            homeViewModel.emitEdgeAdvances(selected.type)
                            viewModel.updateEdgeState { it.copy(notchType = selected.type) }
                            showNotchTypeUi(selected.type)
                        }
                    }
                }
            }
        }
    }

    private fun showNotchTypeUi(type: Advanced) = with (binding) {
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

        requestUpdateLayout()
    }

    private fun requestUpdateLayout() {
       binding.root.post { (parentFragment as? HomeFragment)?.onChildContentExpanded() }
    }
}