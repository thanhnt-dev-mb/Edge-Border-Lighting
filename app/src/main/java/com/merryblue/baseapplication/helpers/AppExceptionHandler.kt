package com.merryblue.baseapplication.helpers

import android.content.Context
import android.content.Intent
import com.merryblue.baseapplication.ui.home.HomeActivity
import timber.log.Timber


class AppExceptionHandler(
    private val context: Context,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Timber.tag("AppExceptionHandler").i("Exception: ${throwable.message}")
            val intent = Intent(context, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(
                context,
                HomeActivity::class.java
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
    }
}
