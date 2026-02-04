package com.merryblue.baseapplication.ui.widget

import android.app.Dialog
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.merryblue.baseapplication.databinding.BottomSheetNoInternetBinding
import org.app.core.base.BaseBottomSheetFragment

class BottomSheetNoInternet: BaseBottomSheetFragment<BottomSheetNoInternetBinding>() {

    private var onPermission: (() -> Unit)? = null

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return

        BottomSheetBehavior.from(bottomSheet).apply {
            isHideable = false
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        return dialog
    }

    override fun initDialog() {
        binding.btnCheckNetwork.setOnClickListener {
            onPermission?.invoke()
            dismiss()
        }
    }

    companion object {
        const val TAG = "bottom_sheet_no_internet"

        fun newInstance(onPermission: () -> Unit) = BottomSheetNoInternet().apply {
            this.onPermission = onPermission
        }
    }
}