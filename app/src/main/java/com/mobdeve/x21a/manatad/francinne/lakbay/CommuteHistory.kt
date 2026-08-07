package com.mobdeve.x21a.manatad.francinne.lakbay

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class CommuteHistory(
    @ColumnInfo(name = "origin") val origin: String,
    @ColumnInfo(name = "destination") val destination: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "duration") val duration: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)