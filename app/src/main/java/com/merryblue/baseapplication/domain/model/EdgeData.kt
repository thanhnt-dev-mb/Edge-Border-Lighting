package com.merryblue.baseapplication.domain.model

import com.merryblue.baseapplication.BuildConfig

sealed class ThemeUi {
    data class Custom(
        val id: String
    ) : ThemeUi()
}

data class EdgeData(
    val version: Int,
    val topics: List<Topic>
)

data class Topic(
    val topicKey: String,
    val module: String,
    val type: String,
    val premium: Boolean,
    val items: List<Item>
)

data class Item(
    val id: String,
    val index: Int,
    val path: String,
    val premium: Boolean,
    val thumbPath: String? = null,
    val colors: List<String>? = null
): ThemeUi()

fun Item.getThumbUrl(): String {
    return "file:///android_asset/${thumbPath ?: path}"
}

fun Item.getFullImageUrl(): String {
    return "${BuildConfig.BASE_URL}/$path"
}