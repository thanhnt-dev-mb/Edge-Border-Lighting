package com.merryblue.baseapplication.ui.widget

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.databinding.BottomSheetEdgePermissionBinding
import com.merryblue.baseapplication.helpers.applyFullScreenMode
import com.merryblue.baseapplication.helpers.setupFullScreen
import com.merryblue.baseapplication.ui.wallpaper.EdgePermissionViewModel

class BottomSheetEdgePermission : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEdgePermissionBinding? = null
    val binding get() = _binding!!

    private val edgePermissionViewModel: EdgePermissionViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setupFullScreen()
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.applyFullScreenMode()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetEdgePermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnAllowPermission.setOnClickListener {
            edgePermissionViewModel.navigateSetting()
            dismiss()
        }
    }

    companion object {
        const val TAG = "bottom_sheet_edge_permission"
        fun newInstance() = BottomSheetEdgePermission()
    }
}