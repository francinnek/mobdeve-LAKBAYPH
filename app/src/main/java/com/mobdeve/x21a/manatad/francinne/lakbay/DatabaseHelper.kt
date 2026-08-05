/*
package com.mobdeve.x21a.manatad.francinne.lakbay

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "lakbay.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_ROUTES = "routes"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DETAILS = "details"
        private const val COLUMN_TIME_WINDOW = "time_window"
        private const val COLUMN_DURATION = "duration"
        private const val COLUMN_FARE = "fare"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createRoutesTable = ("CREATE TABLE " + TABLE_ROUTES + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_DETAILS + " TEXT, "
                + COLUMN_TIME_WINDOW + " TEXT, "
                + COLUMN_DURATION + " TEXT, "
                + COLUMN_FARE + " TEXT" + ")")
        db?.execSQL(createRoutesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_ROUTES")
        onCreate(db)
    }

    @Synchronized
    fun insertRoute(route: Route): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DETAILS, route.details)
            put(COLUMN_TIME_WINDOW, route.timeWindow)
            put(COLUMN_DURATION, route.duration)
            put(COLUMN_FARE, route.fare)
        }
        val id = db.insert(TABLE_ROUTES, null, values)
        db.close()
        return id
    }

    @Synchronized
    fun getAllRoutes(): List<Route> {
        val routeList = mutableListOf<Route>()
        val selectQuery = "SELECT * FROM $TABLE_ROUTES"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val details = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILS))
                val timeWindow = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_WINDOW))
                val duration = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DURATION))
                val fare = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FARE))

                routeList.add(Route(details, timeWindow, duration, fare))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return routeList
    }
}
*/
