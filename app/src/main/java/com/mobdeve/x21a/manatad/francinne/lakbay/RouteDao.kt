package com.mobdeve.x21a.manatad.francinne.lakbay

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes")
    fun getAllRoutes(): List<Route>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRoute(route: Route)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(routes: List<Route>)
}
