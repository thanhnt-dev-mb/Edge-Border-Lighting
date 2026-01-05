package com.merryblue.baseapplication.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.merryblue.baseapplication.helpers.PostRemindNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.scopes.ServiceScoped
import timber.log.Timber

@AndroidEntryPoint
@ServiceScoped
@SuppressLint("LogNotTimber")
class FCMService : FirebaseMessagingService() {

    private var isServiceBound = false

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val powerManager = this.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakelock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mbapplock:scheduled.notification.receiver")
        wakelock?.acquire(3000)

    }

    override fun onNewToken(token: String) {
        Log.d("FCMService", "Refreshed token: $token")
    }

    private fun scheduleJob() {
        val work = OneTimeWorkRequest.Builder(PostRemindNotificationWorker::class.java)
            .build()
        WorkManager.getInstance(this)
            .beginWith(work)
            .enqueue()
    }
}
