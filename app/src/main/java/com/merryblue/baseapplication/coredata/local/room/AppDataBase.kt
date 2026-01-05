package com.merryblue.baseapplication.coredata.local.room

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.merryblue.baseapplication.coredata.local.room.dao.ExampleDao
import com.merryblue.baseapplication.coredata.local.room.entity.ExampleEntity
import java.util.*

@Database(
    entities = [
        ExampleEntity::class
               ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDataBase : RoomDatabase() {
    abstract fun exampleDao(): ExampleDao
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}