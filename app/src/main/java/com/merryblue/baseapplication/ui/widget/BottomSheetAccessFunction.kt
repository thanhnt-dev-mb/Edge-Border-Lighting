package com.merryblue.baseapplication.ui.widget

import com.merryblue.baseapplication.databinding.BottomSheetOptionBinding
import org.app.core.base.BaseBottomSheetFragment

class BottomSheetAccessFunction(
    val title: String? = null,
    private val onCompleted: () -> Unit
) : BaseBottomSheetFragment<BottomSheetOptionBinding>() {

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