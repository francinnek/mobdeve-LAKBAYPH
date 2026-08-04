package com.mobdeve.x21a.manatad.francinne.lakbay

import java.io.InputStream

class GtfsRouteParser {

    // Parse GTFS routes.txt stream into Route objects off the UI thread
    fun parseRoutes(inputStream: InputStream): List<Route> {
        val routesList = mutableListOf<Route>()
        val reader = inputStream.bufferedReader()

        reader.useLines { lines ->
            lines.drop(1).forEach { line -> // Skip CSV header row
                val tokens = line.split(",")
                if (tokens.size >= 4) {
                    val routeShortName = tokens[1].replace("\"", "").trim()
                    val routeLongName = tokens[2].replace("\"", "").trim()
                    val routeType = tokens[3].trim()

                    // Map GTFS route types/names to UI model format
                    val details = "$routeShortName - $routeLongName"
                    val timeWindow = "Regular Operating Hours"
                    val duration = "Est. 30 mins"
                    val fare = "₱20.00"

                    routesList.add(Route(details, timeWindow, duration, fare))
                }
            }
        }
        return routesList
    }
}