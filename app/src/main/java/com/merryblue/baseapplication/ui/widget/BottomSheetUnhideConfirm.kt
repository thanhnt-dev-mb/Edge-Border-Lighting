package com.merryblue.baseapplication.ui.widget

import com.merryblue.baseapplication.databinding.BottomSheetUnhideBinding
import org.app.core.base.BaseBottomSheetFragment
import org.app.core.base.binding.setOnSingleClickListener

class BottomSheetUnhideConfirm(
    private val onCompleted: () -> Unit
) : BaseBottomSheetFragment<BottomSheetUnhideBinding>() {

    override fun initDialog() {
        binding.btnCancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.unhideBtn.setOnSingleClickListener {
            onCompleted.invoke()
            dismissAllowingStateLoss()
        }
    }

    companion object {
        val TAG = "bottom_sheet_unhide"
    }
}