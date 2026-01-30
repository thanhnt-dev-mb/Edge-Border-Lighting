package com.merryblue.baseapplication.ui.widget

import com.merryblue.baseapplication.databinding.BottomSheetEdgePermissionBinding
import org.app.core.base.BaseBottomSheetFragment

class BottomSheetEdgePermission(val onPermission: () -> Unit): BaseBottomSheetFragment<BottomSheetEdgePermissionBinding>() {
    override fun initDialog() {
        binding.btnAllowPermission.setOnClickListener {
            onPermission.invoke()
            dismiss()
        }
    }

    companion object {
        const val TAG = "bottom_sheet_edge_permission"
    }
}