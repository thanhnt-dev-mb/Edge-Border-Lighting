package com.merryblue.baseapplication.ui.widget

import com.merryblue.baseapplication.databinding.BottomSheetEdgePermissionBinding
import org.app.core.base.BaseBottomSheetFragment

class BottomSheetEdgePermission : BaseBottomSheetFragment<BottomSheetEdgePermissionBinding>() {

    private var onPermission: (() -> Unit)? = null

    override fun initDialog() {
        binding.btnAllowPermission.setOnClickListener {
            onPermission?.invoke()
            dismiss()
        }
    }

    companion object {
        const val TAG = "bottom_sheet_edge_permission"

        fun newInstance(onPermission: () -> Unit) = BottomSheetEdgePermission().apply {
            this.onPermission = onPermission
        }
    }
}