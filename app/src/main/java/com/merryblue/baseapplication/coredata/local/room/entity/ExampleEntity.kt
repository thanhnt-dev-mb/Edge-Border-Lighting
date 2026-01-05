package com.merryblue.baseapplication.coredata.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "example_tbl")
data class ExampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var spinWheelId: Long = 0,
    val title: String,
    val color: String,
    var index: Int
)
