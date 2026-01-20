package com.merryblue.baseapplication.domain.model

import com.merryblue.baseapplication.BuildConfig

sealed class ThemeUi {
    data class Custom(
        val id: String = "id"
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
): ThemeUi() {
    val thumbUrl: String = "${BuildConfig.BASE_URL}/${thumbPath ?: path}"
    val pathUrl: String = "${BuildConfig.BASE_URL}/$path"
}

