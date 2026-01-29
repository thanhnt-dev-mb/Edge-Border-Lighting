package com.merryblue.baseapplication.helpers

import android.app.Dialog
import android.content.Context
import com.merryblue.baseapplication.R

object AppLoading {

    private var dialog: Dialog? = null

    var isShowing = false
        private set

    fun displayLoading(context: Context) {
        if (isShowing) {
            return
        }
        isShowing = true
        runCatching {
            if (dialog == null) {
                dialog = context.createDialog().apply {
                    setCancelable(false)
                    setContentView(R.layout.layout_app_loading_global)
                }
            }
            dialog?.showFullScreen()
        }
    }

    fun closeLoading() {
        isShowing = false
        dialog ?: return
        dialog?.dismiss()
        dialog = null
    }
}