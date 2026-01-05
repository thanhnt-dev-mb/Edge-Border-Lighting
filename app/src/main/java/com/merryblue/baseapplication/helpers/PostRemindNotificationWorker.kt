package com.merryblue.baseapplication.helpers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.NotificationModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class PostRemindNotificationWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted val params: WorkerParameters,
) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            Timber.i("PostRemindNotificationWorker", "Worker triggered at ${System.currentTimeMillis()}")
            val content = "TODO"
            val appName = context.getString(R.string.app_name)
            val notificationModel = NotificationModel(
                context.getString(R.string.app_name),
                content,
                appName + "_Notification_Channel",
                (1..1000).random(),
                0,
                null,
            )
            Compatibility.postNotificationRetentionApp(context, notificationModel)
        } catch (_: Exception) {
        } finally {
        }

        return Result.success()
    }
}
