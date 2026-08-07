package com.mobdeve.x21a.manatad.francinne.lakbay

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY id DESC")
    fun getAllHistory(): List<CommuteHistory>

    @Insert
    fun insertHistory(history: CommuteHistory)
}