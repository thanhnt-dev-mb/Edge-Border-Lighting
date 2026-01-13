package com.merryblue.baseapplication.ui.home.advanced

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
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
import com.merryblue.baseapplication.coredata.model.edge.HoleType
import com.merryblue.baseapplication.coredata.model.edge.InfinityType
import com.merryblue.baseapplication.databinding.FragmentEdgeAdvancedBinding
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.helpers.mapFloatToRange
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.home.color.EdgeColorIntent
import com.merryblue.baseapplication.ui.home.color.EdgeTab
import com.merryblue.baseapplication.ui.view.CustomSeekBar
import com.merryblue.baseapplication.ui.view.CustomSeekBar.OnProgressChangeListener
import com.merryblue.baseapplication.ui.view.edgelight.EdgeHoleShape
import com.merryblue.baseapplication.ui.view.edgelight.InfinityShape
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment
import timber.log.Timber


@AndroidEntryPoint
class EdgeAdvancedFragment : BaseFragment<FragmentEdgeAdvancedBinding>() {

    private val viewModel: EdgeAdvancedViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var directionAdapter: EdgeDirectionAdapter
    private lateinit var notchTypeAdapter: EdgeNotchTypeAdapter

    override fun getLayoutId(): Int = R.layout.fragment_edge_advanced

    override fun initView(view: View) {
        directionAdapter = EdgeDirectionAdapter { viewModel.selectDirection(it) }
        notchTypeAdapter = EdgeNotchTypeAdapter { viewModel.selectNotchType(it) }

        binding.apply {
            rcvDirection.apply {
                adapter = directionAdapter
                layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
                itemAnimator = null
            }

            rcvNotchType.apply {
                adapter = notchTypeAdapter
                layoutManager = GridLayoutManager(requireContext(), 4)
                setHasFixedSize(true)
                itemAnimator = null
            }
        }

        viewModel.selectDirection(0)
        viewModel.selectNotchType(0)

        registerOnClick()
    }

    private fun registerOnClick() = with(binding) {

        layoutDisplayNotch.apply {
            seekBarNotchWidth.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val fraction = progress.mapFloatToRange(min = 0.25f, max = 0.65f)
                    Timber.tag("Log_DisplayNotch").d("fraction: $fraction")
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayNotch(progress = fraction, DisplayNotch.NOTCH_WIDTH))
                }
            })

            seekBarNotchHeight.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val heightPx = progress.mapFloatToRange(min = 10f.dpToPx, max = 80f.dpToPx)
                    Timber.tag("Log_DisplayNotch").d("heightPx: $heightPx")
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayNotch(progress = heightPx, DisplayNotch.NOTCH_HEIGHT))
                }
            })

            seekBarNotchTopRadius.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val topRadiusPx = progress.mapFloatToRange(min = 0f.dpToPx, max = 40f.dpToPx)
                    Timber.tag("Log_DisplayNotch").d("topRadiusPx: $topRadiusPx")
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayNotch(progress = topRadiusPx, DisplayNotch.NOTCH_TOP_RADIUS))
                }
            })

            seekBarNotchBottomRadius.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val bottomRadiusPx = progress.mapFloatToRange(0f.dpToPx, 60f.dpToPx)
                    Timber.tag("Log_DisplayNotch").d("bottomRadiusPx: $bottomRadiusPx")
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayNotch(progress = bottomRadiusPx, DisplayNotch.NOTCH_BOTTOM_RADIUS))
                }
            })

            seekBarNotchBottomFullness.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    Timber.tag("Log_DisplayNotch").d("bottomFullness: $progress")
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayNotch(progress = progress, DisplayNotch.NOTCH_BOTTOM_FULLNESS))
                }
            })
        }

        layoutDisplayHole.apply {

            val tabButtons = listOf(btnCircleHole, btnRoundHole)
            val checkIcons = listOf(ivCircleHole, ivRoundHole)

            fun selectTab(selectedBtn: View, selectedCheck: AppCompatImageView) {
                tabButtons.forEach { it.setBackgroundResource(R.drawable.bg_tab_color_edge_unselected) }
                checkIcons.forEach { it.setImageResource(R.drawable.ic_check_unselected) }

                selectedBtn.setBackgroundResource(R.drawable.bg_tab_color_edge_selected)
                selectedCheck.setImageResource(R.drawable.ic_check_selected)
            }

            btnCircleHole.setOnClickListener {
                seekBarHoleRadius.visibility = View.VISIBLE
                tvHoleHeight.visibility = View.GONE
                tvHoleWidth.visibility = View.GONE
                seekBarHoleCorner.visibility = View.GONE
                seekBarHoleRoundWidth.visibility = View.GONE
                seekBarHoleRoundHeight.visibility = View.GONE
                selectTab(btnCircleHole, ivCircleHole)
                homeViewModel.emitHoleType(EdgeHoleShape.CIRCLE)
            }

            btnRoundHole.setOnClickListener {
                tvHoleHeight.visibility = View.VISIBLE
                tvHoleWidth.visibility = View.VISIBLE
                seekBarHoleCorner.visibility = View.VISIBLE
                seekBarHoleRoundWidth.visibility = View.VISIBLE
                seekBarHoleRoundHeight.visibility = View.VISIBLE
                seekBarHoleRadius.visibility = View.GONE
                selectTab(btnRoundHole, ivRoundHole)
                homeViewModel.emitHoleType(EdgeHoleShape.ROUND)
            }

            seekBarHoleLeft.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    Timber.tag("Log_DisplayHole").d("HoleOffsetX: $progress")
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = progress, DisplayHole.HOLE_HORIZONTAL))
                }
            })

            // Up - Down
            seekBarHoleTop.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    Timber.tag("Log_DisplayHole").d("HoleOffsetY: $progress")
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = progress, DisplayHole.HOLE_VERTICAL))
                }
            })

            // Circle Radius
            seekBarHoleRadius.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val r = progress.mapFloatToRange(6f.dpToPx, 30f.dpToPx)
                    Timber.tag("Log_DisplayHole").d("HoleCircleRadius: $r")
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = r, DisplayHole.HOLE_RADIUS))
                }
            })

            seekBarHoleRoundWidth.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val w = progress.mapFloatToRange(40f.dpToPx, 180f.dpToPx)
                    Timber.tag("Log_DisplayHole").d("HoleRoundWidth: $w")
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = w, DisplayHole.HOLE_WIDTH))
                }
            })

            seekBarHoleRoundHeight.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val h = progress.mapFloatToRange(16f.dpToPx, 80f.dpToPx)
                    Timber.tag("Log_DisplayHole").d("HoleRoundHeight: $h")
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = h, DisplayHole.HOLE_HEIGHT))
                }
            })

            seekBarHoleCorner.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val cr = progress.mapFloatToRange(0f.dpToPx, 40f.dpToPx)
                    Timber.tag("Log_DisplayHole").d("RoundCornerRadius: $cr")
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayHole(progress = cr, DisplayHole.HOLE_CORNER))
                }
            })
        }

        layoutDisplayInfinity.apply {
            val tabButtons = listOf(btnInfinityU, btnInfinityV)
            val checkIcons = listOf(ivInfinityU, ivInfinityV)

            fun selectTab(selectedBtn: View, selectedCheck: AppCompatImageView) {
                tabButtons.forEach { it.setBackgroundResource(R.drawable.bg_tab_color_edge_unselected) }
                checkIcons.forEach { it.setImageResource(R.drawable.ic_check_unselected) }

                selectedBtn.setBackgroundResource(R.drawable.bg_tab_color_edge_selected)
                selectedCheck.setImageResource(R.drawable.ic_check_selected)
            }

            btnInfinityU.setOnClickListener {
                selectTab(btnInfinityU, ivInfinityU)
                homeViewModel.emitInfinityType(InfinityShape.U)
            }

            btnInfinityV.setOnClickListener {
                selectTab(btnInfinityV, ivInfinityV)
                homeViewModel.emitInfinityType(InfinityShape.V)
            }

            seekBarInfinityWidth.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val value = progress.mapFloatToRange(60f.dpToPx, 360f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayInfinity(progress = value, DisplayInfinity.INFINITY_WIDTH))
                }
            })

            seekBarInfinityHeight.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val value = progress.mapFloatToRange(0f.dpToPx, 140f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayInfinity(progress = value, DisplayInfinity.INFINITY_HEIGHT))
                }
            })

            seekBarInfinityTop.setOnProgressChangeListener(object : OnProgressChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
                    if (!fromUser) return
                    val value = progress.mapFloatToRange(0f.dpToPx, 60f.dpToPx)
                    homeViewModel.emitEdgeDisplayNotchType(DisplayNotchType.TypeDisplayInfinity(progress = value, DisplayInfinity.INFINITY_TOP))
                }
            })
        }
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

        binding.lnAdvanced.requestLayout()
    }
}