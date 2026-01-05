package com.merryblue.baseapplication.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.merryblue.baseapplication.helpers.PostRemindNotificationWorker
import java.util.*
import java.util.concurrent.TimeUnit

class AlarmService(private val context: Context) {
    private val ALARM_REQUEST_CODE = 2002

    private val alarmManager: AlarmManager? = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private val intent = Intent(context, AlarmReceiver::class.java)
    private val pendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun setRepetitiveAlarm(data: List<Pair<Int, Int>>) {
        val currentCalendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }
        data.forEach {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, it.first)
                set(Calendar.MINUTE, it.second)
                set(Calendar.SECOND, 0)
            }

            if (calendar.after(currentCalendar)) {
                alarmManager?.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        }
    }

    fun setRepetitiveAlarmAt(hour: Int, minute: Int) {
        alarmManager ?: return
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (Calendar.getInstance().before(calendar)) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            calendar.add(Calendar.DATE, 1)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun setupWorker(hour: Int, minute: Int) {
        // Start worker
        val tag = "worker_daily_$hour" + "_" + "$minute"
        val workManager = WorkManager.getInstance(context)
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val initialDelay: Long = if (now.after(calendar)) {
            calendar.add(Calendar.DATE, 1)
            calendar.timeInMillis - now.timeInMillis
        } else {
            calendar.timeInMillis - now.timeInMillis
        }

        val workRequest = PeriodicWorkRequestBuilder<PostRemindNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(tag)
            .build()

        workManager.enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }

    fun configureAlarmAndWorker() {
        setRepetitiveAlarmAt(12, 0)
//        setupWorker(20, 0)
    }
}
