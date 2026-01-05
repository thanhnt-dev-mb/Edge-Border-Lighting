package com.merryblue.baseapplication.coredata.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.merryblue.baseapplication.coredata.local.room.entity.ExampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExampleDao : BaseDao<ExampleEntity> {
    @Query("Select * from example_tbl")
    fun getAll(): Flow<List<ExampleEntity>>
}
