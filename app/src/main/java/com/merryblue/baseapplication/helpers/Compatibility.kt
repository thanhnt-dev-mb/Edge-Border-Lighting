package com.merryblue.baseapplication.helpers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.NotificationModel
import com.merryblue.baseapplication.ui.home.HomeActivity

class Compatibility {
    companion object {
    
        private val permissions = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    
        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
        private var permissions33 = mutableListOf(
            Manifest.permission.READ_MEDIA_IMAGES,
        )
    
        fun externalPermissions(): MutableList<String> {
            val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions33
            } else {
                permissions
            }
            return p
        }
        
        // For Android 13
        fun requestPostNotificationsPermission(fragment: Fragment, code: Int) {
            fragment.requestPermissions(
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                code
            )
        }

        fun hasPostNotificationsPermission(context: Context, permission: String): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            } else true
        }

        private fun createNotificationChannel(context: Context, id: String): NotificationChannel {
            val channelName: CharSequence = "Video Downloader"
            val channelImportance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(id, channelName, channelImportance)
            notificationChannel.description = "FB Video Downloader"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.setShowBadge(true)

            return notificationChannel
        }

        fun postNotificationRetentionApp(context: Context, data: NotificationModel) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (!notificationManager.areNotificationsEnabled()) { return }
            notificationManager.cancel(data.notificationId)
            val channel = createNotificationChannel(context, data.channelId)
            notificationManager.createNotificationChannel(channel)
            val mBuilder = NotificationCompat.Builder(context, data.channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setNumber(data.badgeNumber)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setShowWhen(true)
                .setAutoCancel(false)
                .setOngoing(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            val notificationIntent = Intent(context, HomeActivity::class.java)

            notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT.or(
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

            mBuilder.setContentIntent(pendingIntent)

//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//                mBuilder.color = ContextCompat.getColor(context, R.color.colorPrimary)
//                val notificationLayout = RemoteViews(context.packageName, R.layout.layout_notification_small)
//                notificationLayout.setTextViewText(R.id.noti_title, data.title)
//                notificationLayout.setTextViewTextSize(R.id.noti_title, TypedValue.COMPLEX_UNIT_SP, 15F)
//                notificationLayout.setTextColor(R.id.noti_title, 0xFF111111.toInt())
//                notificationLayout.setTextViewText(R.id.noti_subtitle, data.content)
//                notificationLayout.setTextColor(R.id.noti_subtitle, context.getColor(R.color.colorPrimary))
//                notificationLayout.setImageViewResource(R.id.noti_right_image, R.drawable.ic_notification_play)
//
//                data.largeImage?.let { image ->
//                    Glide.with(context)
//                        .asBitmap()
//                        .load(image)
//                        .into(object : CustomTarget<Bitmap>() {
//                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                                notificationLayout.setImageViewBitmap(R.id.noti_card_view, resource)
//                                mBuilder.setCustomContentView(notificationLayout)
//                                notificationManager.notify(data.notificationId, mBuilder.build())
//                            }
//
//                            override fun onLoadCleared(placeholder: Drawable?) { notificationManager.notify(data.notificationId, mBuilder.build()) }
//                        })
//                }
//            } else {
                mBuilder
                    .setContentTitle(data.title)
                    .setContentText(data.content)

                data.largeImage?.let { image ->
                    Glide.with(context)
                        .asBitmap()
                        .load(image)
                        .placeholder(R.drawable.app_logo)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                mBuilder.setLargeIcon(resource)
                                notificationManager.notify(data.notificationId, mBuilder.build())
                            }

                            override fun onLoadCleared(placeholder: Drawable?) { notificationManager.notify(data.notificationId, mBuilder.build()) }
                        })
                } ?: kotlin.run { notificationManager.notify(data.notificationId, mBuilder.build()) }
//            }
        }
    }
}
