package com.merryblue.baseapplication.data.dto

import com.google.gson.annotations.SerializedName
import com.merryblue.baseapplication.domain.model.EdgeData
import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.Topic

data class EdgeDataDto(
    @SerializedName("version") val version: Int,
    @SerializedName("topics") val topics: List<TopicDto>
)

data class TopicDto(
    @SerializedName("topicKey") val topicKey: String,
    @SerializedName("module") val module: String,
    @SerializedName("premium") val premium: Boolean,
    @SerializedName("items") val items: List<ItemDto>
)

data class ItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("index") val index: Int,
    @SerializedName("path") val path: String,
    @SerializedName("premium") val premium: Boolean,
    @SerializedName("type") val type: String?,
    @SerializedName("thumbPath") val thumbPath: String? = null,
    @SerializedName("colors") val colors: List<String>? = null
)

fun EdgeDataDto.toDomain(): EdgeData {
    return EdgeData(
        version = version,
        topics = topics.map { it.toDomain() }
    )
}

fun TopicDto.toDomain(): Topic {
    return Topic(
        topicKey = topicKey,
        module = module,
        premium = premium,
        items = items.map { it.toDomain() }
    )
}

fun ItemDto.toDomain(): Item {
    return Item(
        id = id,
        index = index,
        path = path,
        premium = premium,
        type = type ?: "",
        thumbPath = thumbPath,
        colors = colors
    )
}