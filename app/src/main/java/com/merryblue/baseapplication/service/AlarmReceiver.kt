package com.merryblue.baseapplication.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.NotificationModel
import com.merryblue.baseapplication.helpers.Compatibility
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.app.core.base.utils.StringResId
import timber.log.Timber

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    private var isServiceBound = false
    private var ctx: Context? = null

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        ctx = context

        Timber.tag("AlarmReceiver").i("Alarm triggered at ${System.currentTimeMillis()}")
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakelock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mbedgeborderlighting:scheduled.notification.receiver")
        wakelock?.acquire(3000)
        val title = ctx!!.getString(R.string.app_name) + " " + ctx!!.getString(R.string.txt_notification_title)
        val content = ctx!!.getString(R.string.txt_notification_content)
        val appName = ctx!!.getString(R.string.app_name)
        val notificationModel = NotificationModel(
            title,
            content,
            appName + "_Notification_Channel",
            (1..1001).random(),
            0,
            null,
            1,
        )

        Compatibility.postNotificationRetentionApp(ctx!!, notificationModel)
    }
}
