package com.merryblue.baseapplication.domain.repository

import com.merryblue.baseapplication.domain.model.Item
import com.merryblue.baseapplication.domain.model.Topic

interface EdgeDataRepository {
    suspend fun getItems(topicKey: String, page: Int): List<Item>
    fun getDataTopic(topicKey: String): Topic?
    fun getFullImageUrl(path: String): String
    fun getAllTopics(): List<Topic>
    fun getTopicsByModule(module: String): List<Topic>
    fun getPremiumTopics(): List<Topic>
    fun getFreeTopics(): List<Topic>
    fun reloadData()
}