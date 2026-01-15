package com.merryblue.baseapplication.domain.repository

import com.merryblue.baseapplication.domain.model.Topic

interface EdgeDataRepository {
    fun getDataTopic(topicKey: String): Topic?
    fun getFullImageUrl(path: String): String
    fun getAllTopics(): List<Topic>
    fun getTopicsByModule(module: String): List<Topic>
    fun getTopicsByType(type: String): List<Topic>
    fun getPremiumTopics(): List<Topic>
    fun getFreeTopics(): List<Topic>
    fun reloadData()
}