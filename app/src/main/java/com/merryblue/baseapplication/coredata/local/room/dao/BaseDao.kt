package com.merryblue.baseapplication.coredata.local.room.dao

import androidx.room.*

interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: T): Long

    @Insert
    suspend fun insertAll(vararg items: T)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(items: List<T>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(vararg items: T)

    @Update
    suspend fun update(item: T)

    @Update
    suspend fun updateAll(items: List<T>)

    @Delete
    suspend fun delete(item: T)

    @Delete
    suspend fun deleteAll(vararg items: T)
}
