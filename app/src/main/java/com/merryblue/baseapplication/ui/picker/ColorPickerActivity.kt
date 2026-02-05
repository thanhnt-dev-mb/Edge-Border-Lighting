package com.merryblue.baseapplication.ui.picker

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityColorPickerBinding
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import com.merryblue.baseapplication.helpers.parseHexSafe
import com.merryblue.baseapplication.helpers.toHex
import com.merryblue.baseapplication.service.edge.EdgeLightingOverlayService
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.home.color.EdgeTab
import com.merryblue.baseapplication.ui.wallpaper.EdgePermissionViewModel
import com.merryblue.baseapplication.ui.widget.BottomSheetEdgePermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.toastMsg
import kotlin.getValue
import androidx.core.graphics.toColorInt
import com.merryblue.baseapplication.helpers.openProperNetworkSettings
import com.merryblue.baseapplication.ui.widget.BottomSheetNoInternet
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ColorPickerActivity : BaseActivity<ActivityColorPickerBinding>() {

    private val prefs by lazy { AppPreferences(this) }
    private val viewModel: HomeViewModel by viewModels()
    private val edgePermissionViewModel: EdgePermissionViewModel by viewModels()
    private val overlayPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(this)) {
            startEdgeOverlay()
        } else {
            prefs.edgeState = prefs.edgeState.copy(isEnableEdgeLighting = false)
            finish()
        }
    }

    private val colors = intArrayOf("#AA00FF".toColorInt(), "#2962FF".toColorInt(), "#00C853".toColorInt(), "#FF3D00".toColorInt())
    private var selectedIndex = 0
    private var isProgrammaticChange = false
    private var isDraggingPicker = false
    private var currentTab: EdgeTab = EdgeTab.TAB_4
    private var currentColor: IntArray = intArrayOf(*colors)

    override fun getLayoutId(): Int = R.layout.activity_color_picker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
    }

    override fun setUpViews() {
        initViews()
        setupClampOnFocusOut()
        registerChangedListener()
        registerOnClick()
    }

    override fun setUpObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    edgePermissionViewModel.edgePermission.collect {
                        val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${packageName}".toUri())
                        overlayPermissionLauncher.launch(i)
                    }
                }

                launch {
                    viewModel.connectionState.collectLatest {
                        onNetworkStateChanged(it)
                        handleNoInternetBottomSheet(it)
                    }
                }
            }
        }
    }

    private fun handleNoInternetBottomSheet(isConnected: Boolean) {
        val fm = supportFragmentManager
        val current = fm.findFragmentByTag(BottomSheetNoInternet.TAG) as? BottomSheetDialogFragment

        if (isConnected) {
            if (current?.dialog?.isShowing == true) current.dismissAllowingStateLoss()
            return
        }

        if (current?.dialog?.isShowing == true) return

        BottomSheetNoInternet.newInstance {
            this.openProperNetworkSettings()
        }.show(fm, BottomSheetNoInternet.TAG)
    }

    private fun checkPermissionOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            showBottomSheetEdgePermission()
            return
        }
        startEdgeOverlay()
    }

    private fun startEdgeOverlay() {
        prefs.edgeState = prefs.edgeState.copy(isEnableEdgeLighting = true)
        ContextCompat.startForegroundService(this, Intent(this, EdgeLightingOverlayService::class.java))
        finish()
    }

    private fun showBottomSheetEdgePermission() {
        (supportFragmentManager.findFragmentByTag(BottomSheetEdgePermission.TAG) as? BottomSheetDialogFragment)?.dismissAllowingStateLoss()
        BottomSheetEdgePermission.newInstance().show(supportFragmentManager, BottomSheetEdgePermission.TAG)
    }

    private fun registerOnClick() = with (binding) {

        btnBackPreview.setOnClickListener { finish() }

        lnColor1.setOnClickListener { selectChip(COLOR_1) }
        lnColor2.setOnClickListener { selectChip(COLOR_2) }
        lnColor3.setOnClickListener { selectChip(COLOR_3) }
        lnColor4.setOnClickListener { selectChip(COLOR_4) }

        btnFourColors.setOnClickListener { renderSelectedTab(EdgeTab.TAB_4) }
        btnThreeColors.setOnClickListener { renderSelectedTab(EdgeTab.TAB_3) }
        btnTwoColors.setOnClickListener { renderSelectedTab(EdgeTab.TAB_2) }

        btnApply.setOnClickListener {
            toastMsg(getString(R.string.changes_have_been_applied))
            checkPermissionOverlay()
        }
    }

    private fun initViews() = with (binding) {
        applyToUi()
        selectChip(COLOR_4)
        renderSelectedTab(EdgeTab.TAB_4)

        hueSlider.post {
            isProgrammaticChange = true
            hueSlider.setHue(0f, notify = true)
            svPicker.setHue(0f)
            val c = svPicker.getColor()
            updateFromColor(c, updatePickers = false)
            isProgrammaticChange = false
        }
    }

    private fun renderSelectedTab(tab: EdgeTab) = with(binding) {

        currentTab = tab

        val tabButtons = mapOf(
            EdgeTab.TAB_4 to btnFourColors,
            EdgeTab.TAB_3 to btnThreeColors,
            EdgeTab.TAB_2 to btnTwoColors,
        )
        val checkIcons = mapOf(
            EdgeTab.TAB_4 to ivCheckFour,
            EdgeTab.TAB_3 to ivCheckThree,
            EdgeTab.TAB_2 to ivCheckTwo,
        )

        // reset
        tabButtons.values.forEach { it.setBackgroundResource(R.drawable.bg_tab_color_edge_unselected) }
        checkIcons.values.forEach { it.setImageResource(R.drawable.ic_check_unselected) }

        // select current
        tabButtons[tab]?.setBackgroundResource(R.drawable.bg_tab_color_edge_selected)
        checkIcons[tab]?.setImageResource(R.drawable.ic_check_selected)

        when (tab) {
            EdgeTab.TAB_2 -> {
                lnColor4.visibility = View.GONE
                lnColor3.visibility = View.GONE
                selectChip(COLOR_2)
            }
            EdgeTab.TAB_3 -> {
                lnColor4.visibility = View.GONE
                lnColor3.visibility = View.VISIBLE
                selectChip(COLOR_3)
            }
            EdgeTab.TAB_4 -> {
                lnColor4.visibility = View.VISIBLE
                lnColor3.visibility = View.VISIBLE
                selectChip(COLOR_4)
            }
        }

        updateCurrentColor()
    }

    private fun registerChangedListener() = with (binding) {
        svPicker.setOnDraggingChangedListener { dragging ->
            isDraggingPicker = dragging
            setInputsEnabled(!dragging)
            if (dragging) clearInputsFocus()
        }

        hueSlider.setOnDraggingChangedListener { dragging ->
            isDraggingPicker = dragging
            setInputsEnabled(!dragging)
            if (dragging) clearInputsFocus()
        }

        hueSlider.setHueChangedListener { hue ->
            svPicker.setHue(hue)
        }

        svPicker.setOnColorChangedListener { color ->
            if (isProgrammaticChange) return@setOnColorChangedListener
            isProgrammaticChange = true
            updateFromColor(color, updatePickers = false)
            isProgrammaticChange = false
        }

        edtHex.doAfterTextChanged { s ->
            if (isProgrammaticChange || isDraggingPicker) return@doAfterTextChanged
            val text = s?.toString().orEmpty()
            val c = text.parseHexSafe ?: return@doAfterTextChanged

            isProgrammaticChange = true
            updateFromColor(c, updatePickers = true)
            isProgrammaticChange = false
        }

        val rgbWatcher: (Unit) -> Unit = { actionRGBWatcher() }
        edtRValue.doAfterTextChanged { rgbWatcher.invoke(Unit) }
        edtGValue.doAfterTextChanged { rgbWatcher.invoke(Unit) }
        edtBValue.doAfterTextChanged { rgbWatcher.invoke(Unit) }
    }

    private fun actionRGBWatcher() {
        if (isProgrammaticChange || isDraggingPicker) return
        val r = binding.edtRValue.text.toString().toIntOrNull() ?: return
        val g = binding.edtGValue.text.toString().toIntOrNull() ?: return
        val b = binding.edtBValue.text.toString().toIntOrNull() ?: return

        val rr = r.coerceIn(0, 255)
        val gg = g.coerceIn(0, 255)
        val bb = b.coerceIn(0, 255)
        val c = Color.rgb(rr, gg, bb)

        isProgrammaticChange = true
        updateFromColor(c, updatePickers = true)
        isProgrammaticChange = false
    }

    private fun setupClampOnFocusOut() = with(binding) {
        edtRValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) clampRgbAndApply(from = edtRValue)
        }
        edtGValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) clampRgbAndApply(from = edtGValue)
        }
        edtBValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) clampRgbAndApply(from = edtBValue)
        }

        edtHex.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) clampHexAndApply()
        }
    }

    private fun clampRgbAndApply(from: AppCompatEditText) = with(binding) {
        if (isProgrammaticChange) return

        fun clampField(et: AppCompatEditText) {
            val v = et.text?.toString()?.trim().orEmpty().toIntOrNull()
            val clamped = (v ?: 0).coerceIn(0, 255)
            if (et.text?.toString() != clamped.toString()) {
                et.setText(clamped.toString())
            }
        }

        isProgrammaticChange = true
        clampField(edtRValue)
        clampField(edtGValue)
        clampField(edtBValue)

        val r = edtRValue.text.toString().toIntOrNull() ?: 0
        val g = edtGValue.text.toString().toIntOrNull() ?: 0
        val b = edtBValue.text.toString().toIntOrNull() ?: 0

        val c = Color.rgb(r, g, b)
        updateFromColor(c, updatePickers = true)
        isProgrammaticChange = false
    }

    private fun clampHexAndApply() = with(binding) {
        if (isProgrammaticChange) return

        val raw = edtHex.text?.toString().orEmpty().trim()
        val parsed = raw.parseHexSafe

        isProgrammaticChange = true
        if (parsed != null) {
            edtHex.setText(parsed.toHex)
            updateFromColor(parsed, updatePickers = true)
        } else {
            val c = svPicker.getColor()
            edtHex.setText(c.toHex)
        }
        isProgrammaticChange = false
    }

    private fun setInputsEnabled(enabled: Boolean) = with(binding) {
        setEditEnabled(edtRValue, enabled)
        setEditEnabled(edtGValue, enabled)
        setEditEnabled(edtBValue, enabled)
        setEditEnabled(edtHex, enabled)
    }

    private fun setEditEnabled(et: AppCompatEditText, enabled: Boolean) {
        et.isEnabled = enabled
        et.isFocusable = enabled
        et.isFocusableInTouchMode = enabled
        et.isCursorVisible = enabled
        et.isLongClickable = enabled
        et.setTextIsSelectable(enabled)
        et.alpha = if (enabled) 1f else 0.6f
    }

    private fun clearInputsFocus() = with(binding) {
        edtRValue.clearFocus()
        edtGValue.clearFocus()
        edtBValue.clearFocus()
        edtHex.clearFocus()
        binding.main.requestFocus()
    }

    private fun updateFromColor(color: Int, updatePickers: Boolean) = with(binding) {
        if (updatePickers) {
            hueSlider.setFromColor(color)
            svPicker.setFromColor(color)
        }

        if (!edtHex.hasFocus()) {
            val hex = color.toHex
            if (edtHex.text?.toString() != hex) edtHex.setText(hex)
        }

        if (!edtRValue.hasFocus()) edtRValue.setText(Color.red(color).toString())
        if (!edtGValue.hasFocus()) edtGValue.setText(Color.green(color).toString())
        if (!edtBValue.hasFocus()) edtBValue.setText(Color.blue(color).toString())

        colors[selectedIndex] = color
        applyToUi()
    }

    private fun selectChip(index: Int) = with(binding) {
        selectedIndex = index

        val chips = listOf(chipColor1, chipColor2, chipColor3, chipColor4)
        val labels = listOf(tvColor1, tvColor2, tvColor3, tvColor4)

        chips.forEachIndexed { i, v ->
            if (i == index) v.setBackgroundResource(R.drawable.bg_select_color_preview)
            else v.background = null
        }

        val white = ContextCompat.getColor(this@ColorPickerActivity, R.color.colorWhite)
        val gray = ContextCompat.getColor(this@ColorPickerActivity, R.color.colorGray04)
        labels.forEachIndexed { i, tv -> tv.setTextColor(if (i == index) white else gray) }

        val c = colors[index]
        isProgrammaticChange = true
        updateFromColor(c, updatePickers = true)
        isProgrammaticChange = false
    }

    private fun applyToUi() = with(binding) {
        viewColor1.backgroundTintList = ColorStateList.valueOf(colors[COLOR_1])
        viewColor2.backgroundTintList = ColorStateList.valueOf(colors[COLOR_2])
        viewColor3.backgroundTintList = ColorStateList.valueOf(colors[COLOR_3])
        viewColor4.backgroundTintList = ColorStateList.valueOf(colors[COLOR_4])
        updateCurrentColor()
    }

    private fun updateCurrentColor() {
        currentColor = when (currentTab) {
            EdgeTab.TAB_2 -> intArrayOf(colors[COLOR_1], colors[COLOR_2])
            EdgeTab.TAB_3 -> intArrayOf(colors[COLOR_1], colors[COLOR_2], colors[COLOR_3])
            EdgeTab.TAB_4 -> intArrayOf(colors[COLOR_1], colors[COLOR_2], colors[COLOR_3], colors[COLOR_4])
        }

        binding.edgeViewPreview.setColors(*currentColor)
        prefs.edgeState = prefs.edgeState.copy(colors = currentColor)
        viewModel.sendActionBroadcast(ACTION_EDGE_OVERLAY_CHANGED)
    }

    companion object {
        private const val COLOR_1 = 0
        private const val COLOR_2 = 1
        private const val COLOR_3 = 2
        private const val COLOR_4 = 3
    }
}
