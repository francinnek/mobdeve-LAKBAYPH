package com.mobdeve.x21a.manatad.francinne.lakbay

import java.io.InputStream

class GtfsParser {
    fun splitCsv(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (char in line) {
            if (char == '\"') {
                inQuotes = !inQuotes
            } else if (char == ',' && !inQuotes) {
                result.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(char)
            }
        }
        result.add(sb.toString().trim())
        return result
    }

    // Parse GTFS stops stream off the main thread
    fun parseStops(inputStream: InputStream): List<GtfsStop> {
        val stopsList = mutableListOf<GtfsStop>()
        val reader = inputStream.bufferedReader()

        reader.useLines { lines ->
            lines.drop(1).forEach { line ->
                val tokens = splitCsv(line)
                if (tokens.size >= 6) {
                    val stopId = tokens[0].trim()
                    val stopName = tokens[2].replace("\"", "").trim()
                    val lat = tokens[4].trim().toDoubleOrNull() ?: 0.0
                    val lng = tokens[5].trim().toDoubleOrNull() ?: 0.0

                    stopsList.add(GtfsStop(stopId, stopName, lat, lng))
                }
            }
        }
        return stopsList
    }

    fun getRouteDetailsMap(
        stopsStream: InputStream,
        tripsStream: InputStream,
        stopTimesStream: InputStream
    ): Map<String, String> {
        // 1. Map StopID -> StopName
        val stopNames = stopsStream.bufferedReader().useLines { lines ->
            lines.drop(1).associate { line ->
                val tokens = splitCsv(line)
                tokens[0] to tokens[2].replace("\"", "").split(",")[0].trim()
            }
        }

        // 2. Map TripID -> RouteID
        val tripToRoute = tripsStream.bufferedReader().useLines { lines ->
            lines.drop(1).associate { line ->
                val tokens = splitCsv(line)
                tokens[10] to tokens[0] // trip_id to route_id
            }
        }

        // 3. Map RouteID -> List of Stop Names
        val routeStops = mutableMapOf<String, MutableList<String>>()
        stopTimesStream.bufferedReader().useLines { lines ->
            lines.drop(1).forEach { line ->
                val tokens = splitCsv(line)
                val tripId = tokens[0]
                val routeId = tripToRoute[tripId] ?: return@forEach
                val stopName = stopNames[tokens[2]] ?: "Unknown Stop"

                if (!routeStops.containsKey(routeId)) routeStops[routeId] = mutableListOf()
                if (!routeStops[routeId]!!.contains(stopName)) {
                    routeStops[routeId]!!.add(stopName)
                }
            }
        }

        // 4. Convert to descriptive strings: "Stop A > Stop B > Stop C"
        return routeStops.mapValues { it.value.joinToString(" > ") }
    }

    // Return mapping of route_id -> list of GtfsStop (with coordinates) so callers can spatially filter routes
    fun getRouteStopsMap(
        stopsStream: InputStream,
        tripsStream: InputStream,
        stopTimesStream: InputStream
    ): Map<String, List<GtfsStop>> {
        // Map stop_id -> GtfsStop
        val stopsMap = stopsStream.bufferedReader().useLines { lines ->
            lines.drop(1).mapNotNull { line ->
                val tokens = splitCsv(line)
                if (tokens.size < 6) return@mapNotNull null
                val id = tokens[0]
                val name = tokens[2].replace("\"", "").trim()
                val lat = tokens[4].trim().toDoubleOrNull() ?: 0.0
                val lng = tokens[5].trim().toDoubleOrNull() ?: 0.0
                id to GtfsStop(id, name, lat, lng)
            }.toMap()
        }

        // Reuse trip->route mapping logic from above
        val tripToRoute = tripsStream.bufferedReader().useLines { lines ->
            lines.drop(1).associate { line ->
                val tokens = splitCsv(line)
                tokens[10] to tokens[0]
            }
        }

        val routeStops = mutableMapOf<String, MutableList<GtfsStop>>()
        stopTimesStream.bufferedReader().useLines { lines ->
            lines.drop(1).forEach { line ->
                val tokens = splitCsv(line)
                if (tokens.size < 3) return@forEach
                val tripId = tokens[0]
                val routeId = tripToRoute[tripId] ?: return@forEach
                val stopId = tokens[2]
                val stop = stopsMap[stopId] ?: return@forEach

                val list = routeStops.getOrPut(routeId) { mutableListOf() }
                // Avoid duplicates based on stopId
                if (list.none { it.stopId == stop.stopId }) {
                    list.add(stop)
                }
            }
        }

        return routeStops
    }
}