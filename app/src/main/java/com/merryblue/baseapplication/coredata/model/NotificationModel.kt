package com.merryblue.baseapplication.coredata.model

data class NotificationModel(
    val title: String,
    var content: String,
    var channelId: String,
    var notificationId: Int,
    var videoId: Int?,
    var largeImage: String?,
    var badgeNumber: Int = 1,
)
