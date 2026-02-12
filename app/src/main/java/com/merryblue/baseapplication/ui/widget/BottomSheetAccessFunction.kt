package com.merryblue.baseapplication.ui.widget

import android.app.Dialog
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.merryblue.baseapplication.databinding.BottomSheetOptionBinding
import com.merryblue.baseapplication.helpers.applyFullScreenMode
import com.merryblue.baseapplication.helpers.setupFullScreen
import org.app.core.base.BaseBottomSheetFragment

class BottomSheetAccessFunction(
    val title: String? = null,
    private val onCompleted: () -> Unit
) : BaseBottomSheetFragment<BottomSheetOptionBinding>() {

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
        title?.let { binding.headerTv.text = it }
        
        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.btnOpen.setOnClickListener {
            onCompleted.invoke()
            dismissAllowingStateLoss()
        }
    }
}