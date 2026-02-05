package com.merryblue.baseapplication.ui.widget

import android.app.Dialog
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.merryblue.baseapplication.databinding.BottomSheetUnhideBinding
import com.merryblue.baseapplication.helpers.applyFullScreenMode
import com.merryblue.baseapplication.helpers.setupFullScreen
import org.app.core.base.BaseBottomSheetFragment
import org.app.core.base.binding.setOnSingleClickListener

class BottomSheetUnhideConfirm(
    private val onCompleted: () -> Unit
) : BaseBottomSheetFragment<BottomSheetUnhideBinding>() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setupFullScreen()
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.applyFullScreenMode()
    }

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