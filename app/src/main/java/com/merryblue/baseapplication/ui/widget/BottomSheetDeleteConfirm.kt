package com.merryblue.baseapplication.ui.widget

import android.app.Dialog
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.BottomSheetDeleteBinding
import com.merryblue.baseapplication.helpers.applyFullScreenMode
import com.merryblue.baseapplication.helpers.setupFullScreen
import org.app.core.base.BaseBottomSheetFragment
import org.app.core.base.binding.setOnSingleClickListener

class BottomSheetDeleteConfirm(
    private val count: Int,
    private val onCompleted: () -> Unit
) : BaseBottomSheetFragment<BottomSheetDeleteBinding>() {

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
        binding.noticeTv.text = getString(R.string.txt_delete_notice, count)
        binding.btnCancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.deleteBtn.setOnSingleClickListener {
            onCompleted.invoke()
            dismissAllowingStateLoss()
        }
    }

    companion object {
        val TAG = "bottom_sheet_delete"
    }
}