package com.merryblue.baseapplication.data.repoimpl

import android.content.Context
import com.google.gson.Gson
import com.merryblue.baseapplication.BuildConfig
import com.merryblue.baseapplication.data.dto.EdgeDataDto
import com.merryblue.baseapplication.data.dto.toDomain
import com.merryblue.baseapplication.domain.model.EdgeData
import com.merryblue.baseapplication.domain.model.Topic
import com.merryblue.baseapplication.domain.repository.EdgeDataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdgeDataRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : EdgeDataRepository {

    private var edgeData: EdgeData? = null

    init {
        loadData()
    }

    private fun loadData() {
        try {
            val jsonString = context.assets.open("edge_data.json")
                .bufferedReader()
                .use { it.readText() }

            val dto = gson.fromJson(jsonString, EdgeDataDto::class.java)
            edgeData = dto.toDomain()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getDataTopic(topicKey: String): Topic? {
        return edgeData?.topics?.find { it.topicKey == topicKey }
    }

    override fun getFullImageUrl(path: String): String {
        return "${BuildConfig.BASE_URL}/$path"
    }

    override fun getAllTopics(): List<Topic> {
        return edgeData?.topics ?: emptyList()
    }

    override fun getTopicsByModule(module: String): List<Topic> {
        return edgeData?.topics?.filter { it.module == module } ?: emptyList()
    }

    override fun getTopicsByType(type: String): List<Topic> {
        return edgeData?.topics?.filter { it.type == type } ?: emptyList()
    }

    override fun getPremiumTopics(): List<Topic> {
        return edgeData?.topics?.filter { it.premium } ?: emptyList()
    }

    override fun getFreeTopics(): List<Topic> {
        return edgeData?.topics?.filter { !it.premium } ?: emptyList()
    }

    override fun reloadData() {
        loadData()
    }
}