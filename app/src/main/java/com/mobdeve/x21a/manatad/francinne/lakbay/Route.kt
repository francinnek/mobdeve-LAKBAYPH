package com.mobdeve.x21a.manatad.francinne.lakbay

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class Route(
    @ColumnInfo(name = "details") val details: String,
    @ColumnInfo(name = "time_window") val timeWindow: String,
    @ColumnInfo(name = "duration") val duration: String,
    @ColumnInfo(name = "fare") val fare: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)
