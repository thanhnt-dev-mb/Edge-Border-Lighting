package com.merryblue.baseapplication.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.tag("###DEBUG").d("BootReceiver....${intent?.action}")
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                val alarmService = AlarmService(context ?: return)
                alarmService.setRepetitiveAlarmAt(8, 0)
                alarmService.setupWorker(8, 0)
            } catch (_: Exception) {}
        }
    }
}
