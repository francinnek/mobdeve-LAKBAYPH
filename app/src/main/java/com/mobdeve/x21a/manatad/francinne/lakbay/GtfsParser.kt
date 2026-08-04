package com.mobdeve.x21a.manatad.francinne.lakbay

import java.io.InputStream

class GtfsParser {

    // Parse GTFS stops stream off the main thread
    fun parseStops(inputStream: InputStream): List<GtfsStop> {
        val stopsList = mutableListOf<GtfsStop>()
        val reader = inputStream.bufferedReader()

        reader.useLines { lines ->
            lines.drop(1).forEach { line -> // Skip CSV header
                val tokens = line.split(",")
                if (tokens.size >= 4) {
                    val stopId = tokens[0].trim()
                    val stopName = tokens[1].replace("\"", "").trim()
                    val lat = tokens[2].trim().toDoubleOrNull() ?: 0.0
                    val lng = tokens[3].trim().toDoubleOrNull() ?: 0.0

                    stopsList.add(GtfsStop(stopId, stopName, lat, lng))
                }
            }
        }
        return stopsList
    }
}