package com.merryblue.baseapplication.ui.home

import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.edge.Advanced
import com.merryblue.baseapplication.coredata.model.edge.DisplayHole
import com.merryblue.baseapplication.coredata.model.edge.DisplayInfinity
import com.merryblue.baseapplication.coredata.model.edge.DisplayNotch
import com.merryblue.baseapplication.coredata.model.edge.DisplayNotchType
import com.merryblue.baseapplication.coredata.model.edge.EdgeAdvanced
import com.merryblue.baseapplication.coredata.model.edge.EdgePreset
import com.merryblue.baseapplication.coredata.model.edge.EdgeSelection
import com.merryblue.baseapplication.coredata.model.edge.EdgeSettings
import com.merryblue.baseapplication.coredata.model.edge.EdgeStyle
import com.merryblue.baseapplication.coredata.model.edge.InfinityType
import com.merryblue.baseapplication.databinding.FragmentHomeBinding
import com.merryblue.baseapplication.helpers.dpToPx
import com.merryblue.baseapplication.helpers.mapFloatToRange
import com.merryblue.baseapplication.helpers.mapFloatToRangeLong
import com.merryblue.baseapplication.helpers.updateHeightForCurrentPage
import com.merryblue.baseapplication.ui.view.CustomSeekBar
import com.merryblue.baseapplication.ui.view.CustomSeekBar.OnProgressChangeListener
import com.merryblue.baseapplication.ui.view.edgelight.EdgeHoleShape
import com.merryblue.baseapplication.ui.view.edgelight.InfinityShape
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.app.core.base.BaseFragment
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var mediator: TabLayoutMediator

    override fun getLayoutId() = R.layout.fragment_home

    override fun setUpViews() {
//        funcTestDisplayNotch()          // todo: test
//        funcTestDisplayHole()           // todo: test
//        funcTestDisplayInfinity()       // todo: test
        initViewPager()
        initTabLayout()
        registerOnClick()
    }

//    private fun funcTestDisplayInfinity() = binding.apply {
//
//        edgeView.setNotchType(Advanced.NOTCH_DISPLAY_INFINITY)
//        edgeView.setInfinityShapeV()
//
//        btnInfinityU.setOnClickListener {
//            edgeView.setNotchType(Advanced.NOTCH_DISPLAY_INFINITY)
//            edgeView.setInfinityShapeU()
//        }
//
//        btnInfinityV.setOnClickListener {
//            edgeView.setNotchType(Advanced.NOTCH_DISPLAY_INFINITY)
//            edgeView.setInfinityShapeV()
//        }
//
//        seekBarInfinity1.setOnProgressChangeListener(object : OnProgressChangeListener {
//            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
//                if (!fromUser) return
//                edgeView.setInfinityWidthPx(progress.mapFloatToRange(60f.dpToPx, 360f.dpToPx))
//            }
//        })
//
//        seekBarInfinity2.setOnProgressChangeListener(object : OnProgressChangeListener {
//            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
//                if (!fromUser) return
//                edgeView.setInfinityHeightPx(progress.mapFloatToRange(0f.dpToPx, 140f.dpToPx))
//            }
//        })
//
//        seekBarInfinity3.setOnProgressChangeListener(object : OnProgressChangeListener {
//            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
//                if (!fromUser) return
//                edgeView.setInfinityRadiusTopPx(progress.mapFloatToRange(0f.dpToPx, 60f.dpToPx))
//            }
//        })
//
//        seekBarInfinity4.setOnProgressChangeListener(object : OnProgressChangeListener {
//            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Float, fromUser: Boolean) {
//                if (!fromUser) return
////                edgeView.setInfinityRadiusBottomPx(progress.mapFloatToRange(0f.dpToPx, 70f.dpToPx))
//            }
//        })
//    }

//    private fun funcTestDisplayHole() = binding.apply {
//
//        // Init Circle
////        edgeView.setHoleShape(EdgeHoleShape.CIRCLE)
////        edgeView.setHoleCircleRadiusPx(18f.dpToPx)
////        edgeView.setHoleOffsetX(0f)
////        edgeView.setHoleOffsetY(12f.dpToPx)
////        seekBarHole3.visibility = View.VISIBLE
////        seekBarHole4.visibility = View.GONE
////        seekBarHole5.visibility = View.GONE
////        seekBarHole6.visibility = View.GONE
//
//        // Init Round
////        edgeView.setHoleShape(EdgeHoleShape.ROUND)
////        edgeView.setHoleRoundWidthPx(120f.dpToPx)
////        edgeView.setHoleRoundHeightPx(36f.dpToPx)
////        edgeView.setHoleRoundCornerRadiusPx(18f.dpToPx)
////        edgeView.setHoleOffsetX(0f)
////        edgeView.setHoleOffsetY(40f.dpToPx)
////        seekBarHole3.visibility = View.GONE
////        seekBarHole4.visibility = View.VISIBLE
////        seekBarHole5.visibility = View.VISIBLE
////        seekBarHole6.visibility = View.VISIBLE
//
//        btnCircle.setOnClickListener {
//            edgeView.setNotchType(Advanced.NOTCH_DISPLAY_HOLE)
//            edgeView.setHoleShape(EdgeHoleShape.CIRCLE)
//            seekBarHole3.visibility = View.VISIBLE
//            seekBarHole4.visibility = View.GONE
//            seekBarHole5.visibility = View.GONE
//            seekBarHole6.visibility = View.GONE
//        }
//
//        btnRound.setOnClickListener {
//            edgeView.setNotchType(Advanced.NOTCH_DISPLAY_HOLE)
//            edgeView.setHoleShape(EdgeHoleShape.ROUND)
//            seekBarHole3.visibility = View.GONE
//            seekBarHole4.visibility = View.VISIBLE
//            seekBarHole5.visibility = View.VISIBLE
//            seekBarHole6.visibility = View.VISIBLE
//        }
//    }

//    private fun funcTestDisplayNotch() = binding.apply {
//
////        edgeView.setNotchType(Advanced.NOTCH_DISPLAY_NOTCH)
////        edgeView.setNotchWidthFraction(0.35677505f)
////        edgeView.setNotchHeightPx(63.51973f.dpToPx)
////        edgeView.setNotchTopRadiusPx(39.304764f.dpToPx)
////        edgeView.setNotchBottomRadiusPx(29.377974f.dpToPx)
////        edgeView.setNotchBottomFullness(0f)
//    }

    private fun registerOnClick() = with (binding) {
        edgeToggle.setOnCheckedChangeListener { isToggle ->
            edgeView.visibility = if (isToggle) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun initViewPager() = with(binding) {
        vpSettingEdge.adapter = SettingEdgeAdapter(this@HomeFragment)
    }

    private fun initTabLayout() {
        val listSetting = buildList {
            add(getString(R.string.txt_color))
            add(getString(R.string.txt_effect))
            add(getString(R.string.txt_advanced))
        }
        mediator = TabLayoutMediator(binding.tabSettingEdge, binding.vpSettingEdge) { tab, p ->
            tab.text = listSetting[p]
        }
        mediator.attach()

        binding.vpSettingEdge.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.vpSettingEdge.post { binding.vpSettingEdge.updateHeightForCurrentPage() }
            }
        })
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.connectionState.collectLatest { connected ->
                        onNetworkStateChanged(connected)
                    }
                }

                launch {
                    viewModel.edgeColorEvents.drop(1).collect { edgeSelection ->
                        when (edgeSelection) {
                            is EdgeSelection.EdgeColor -> {
                                binding.edgeView.applyPreset(edgeSelection.preset)
                            }

                            is EdgeSelection.EdgeEffect -> {
                                if (edgeSelection.selectedIndex == 0) {
                                    binding.edgeView.setPatternEnabled(false)
                                } else binding.edgeView.applyPreset(edgeSelection.preset)
                            }

                            is EdgeSelection.EdgeAdvanced -> {

                            }
                        }
                    }
                }

                launch {
                    viewModel.edgeSettingsEvents.drop(1).collect { edgeSettings ->
                        when (edgeSettings) {
                            is EdgeSettings.EdgeSpeed -> binding.edgeView.setSpeedMs(edgeSettings.progress)
                            is EdgeSettings.EdgeSize -> binding.edgeView.setSizePx(edgeSettings.progress)
                            is EdgeSettings.EdgeBottomRadius -> binding.edgeView.setBottomRadiusPx(edgeSettings.progress)
                            is EdgeSettings.EdgeTopRadius -> binding.edgeView.setTopRadiusPx(edgeSettings.progress)
                        }
                    }
                }

                launch {
                    viewModel.edgeAdvancedEvents.drop(1).collect { adv ->
                        binding.edgeView.setAdvanced(adv)
                    }
                }

                launch {
                    viewModel.edgeHoleTypeEvents.drop(1).collect { type ->
                        binding.edgeView.setHoleShape(type)
                    }
                }

                launch {
                    viewModel.edgeInfinityTypeEvents.drop(1).collect { type ->
                        binding.edgeView.setInfinityShape(type, false)
                    }
                }

                launch {
                    viewModel.edgeDisplayNotchTypeEvents.drop(1).collect { displayNotchType ->
                        when (displayNotchType) {
                            is DisplayNotchType.TypeDisplayNotch -> {
                                when (displayNotchType.type) {
                                    DisplayNotch.NOTCH_WIDTH -> binding.edgeView.setNotchWidthFraction(displayNotchType.progress)
                                    DisplayNotch.NOTCH_HEIGHT -> binding.edgeView.setNotchHeightPx(displayNotchType.progress)
                                    DisplayNotch.NOTCH_TOP_RADIUS -> binding.edgeView.setNotchTopRadiusPx(displayNotchType.progress)
                                    DisplayNotch.NOTCH_BOTTOM_RADIUS -> binding.edgeView.setNotchBottomRadiusPx(displayNotchType.progress)
                                    DisplayNotch.NOTCH_BOTTOM_FULLNESS -> binding.edgeView.setNotchBottomFullness(displayNotchType.progress)
                                }
                            }

                            is DisplayNotchType.TypeDisplayHole -> {
                                when (displayNotchType.type) {
                                    DisplayHole.HOLE_HORIZONTAL -> binding.edgeView.setHoleOffsetXProgress(displayNotchType.progress)
                                    DisplayHole.HOLE_VERTICAL -> binding.edgeView.setHoleOffsetYProgress(displayNotchType.progress)
                                    DisplayHole.HOLE_RADIUS -> binding.edgeView.setHoleCircleRadiusPx(displayNotchType.progress)
                                    DisplayHole.HOLE_WIDTH -> binding.edgeView.setHoleRoundWidthPx(displayNotchType.progress)
                                    DisplayHole.HOLE_HEIGHT -> binding.edgeView.setHoleRoundHeightPx(displayNotchType.progress)
                                    DisplayHole.HOLE_CORNER -> binding.edgeView.setHoleRoundCornerRadiusPx(displayNotchType.progress)
                                }
                            }

                            is DisplayNotchType.TypeDisplayInfinity -> {
                                when (displayNotchType.type) {
                                    DisplayInfinity.INFINITY_TOP -> binding.edgeView.setInfinityWidthPx(displayNotchType.progress)
                                    DisplayInfinity.INFINITY_WIDTH -> binding.edgeView.setInfinityHeightPx(displayNotchType.progress)
                                    DisplayInfinity.INFINITY_HEIGHT -> binding.edgeView.setInfinityRadiusTopPx(displayNotchType.progress)
                                }
                            }
                        }
                        binding.nestedScrollHome.requestLayout()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        if (::mediator.isInitialized) mediator.detach()
        super.onDestroyView()
    }
}
