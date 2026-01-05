package com.merryblue.baseapplication.ui.widget

import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.BottomSheetDeleteBinding
import org.app.core.base.BaseBottomSheetFragment
import org.app.core.base.binding.setOnSingleClickListener

class BottomSheetDeleteConfirm(
    private val count: Int,
    private val onCompleted: () -> Unit
) : BaseBottomSheetFragment<BottomSheetDeleteBinding>() {

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