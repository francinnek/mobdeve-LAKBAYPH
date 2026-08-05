package com.mobdeve.x21a.manatad.francinne.lakbay

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.InputStream
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private val remoteRoutesList = mutableListOf<Route>()
    private lateinit var mMap: GoogleMap
    private var destinationMarker: com.google.android.gms.maps.model.Marker? = null

    private var destinationName = ""

    private var destinationLat = 0.0
    private var destinationLng = 0.0

    // For the Current Location function in the proposal
    private var currentLat = 0.0
    private var currentLng = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Variables to allow user edit current location text area
    private var originName = "Current Location"
    private var originLat = 0.0
    private var originLng = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        binding.rvRoutes.layoutManager = LinearLayoutManager(this)

        database = AppDatabase.getDatabase(this)

        binding.cvSearch.setOnClickListener {

            if (destinationName.isBlank()) {
                Toast.makeText(
                    this,
                    "Please enter a destination first.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val intent = Intent(this, CommuterActiveTripActivity::class.java)

            intent.putExtra("DESTINATION_NAME", destinationName)
            intent.putExtra("DESTINATION_LAT", destinationLat)
            intent.putExtra("DESTINATION_LNG", destinationLng)

            // intent.putExtra("CURRENT_LAT", currentLat)
            // intent.putExtra("CURRENT_LNG", currentLng)
            intent.putExtra("CURRENT_LAT", originLat)
            intent.putExtra("CURRENT_LNG", originLng)

            //intent.putExtra("ROUTE_TITLE", "Current Location → $destinationName")
            intent.putExtra("ROUTE_TITLE", "$originName → $destinationName")


            startActivity(intent)
        }

        binding.tvToAddress.setOnClickListener {
            showDestinationDialog()
        }

        binding.tvFromAddress.setOnClickListener {
            showOriginDialog()
        }

        binding.fabSuggestRoute.setOnClickListener {
            val intent = Intent(this, SuggestRouteActivity::class.java)
            startActivity(intent)
        }

        binding.ivLogout.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            val sharedPreferences = getSharedPreferences("LakbaySession", MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            val intent = Intent(this, OnLaunchActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showRouteDetailsPopup(route: Route) {
        val detailsMessage = "Route Steps:\n${route.details}\n\n" +
                "Operating Hours: ${route.timeWindow}\n" +
                "Estimated Time: ${route.duration}\n" +
                "Estimated Fare: ${route.fare}"

        AlertDialog.Builder(this)
            .setTitle("Route Options & Details")
            .setMessage(detailsMessage)
            .setPositiveButton("Select Route") { _, _ ->
                if (destinationName.isBlank()) {
                    Toast.makeText(
                        this,
                        "Please enter a destination first.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val intent = Intent(this, CommuterActiveTripActivity::class.java)
                intent.putExtra("DESTINATION_NAME", destinationName)
                intent.putExtra("DESTINATION_LAT", destinationLat)
                intent.putExtra("DESTINATION_LNG", destinationLng)
//                intent.putExtra("CURRENT_LAT", currentLat)
//                intent.putExtra("CURRENT_LNG", currentLng)
                intent.putExtra("CURRENT_LAT", originLat)
                intent.putExtra("CURRENT_LNG", originLng)

                intent.putExtra("ROUTE_DETAILS", route.details)
                intent.putExtra("ROUTE_TIME_WINDOW", route.timeWindow)
                intent.putExtra("ROUTE_DURATION", route.duration)
                intent.putExtra("ROUTE_FARE", route.fare)
                //intent.putExtra("ROUTE_TITLE", "Current Location → $destinationName")
                intent.putExtra("ROUTE_TITLE", "$originName → $destinationName")

                startActivity(intent)
            }
            .setNegativeButton("Choose Another", null)
            .show()
    }

    // Prioritize GTFS file parsing off main thread over old dummy entries
    private fun fetchRecommendedRoutes() {
        lifecycleScope.launch(Dispatchers.IO) {
            val routeDao = database.routeDao()

            // Try parsing GTFS raw feed first
            val gtfsRoutes = parseGtfsRoutesFromRaw()

            // Default radius (meters) to consider a stop "near" origin/destination
            val radiusMeters = 800f

            // If we have GTFS route/stop files, perform spatial filtering
            val parser = GtfsParser()
            var routesToDisplay: List<Route> = listOf()

            try {
                val stopsStream = resources.openRawResource(R.raw.stops)
                val tripsStream = resources.openRawResource(R.raw.trips)
                val stopTimesStream = resources.openRawResource(R.raw.stop_times)

                val routeStopsMap = parser.getRouteStopsMap(stopsStream, tripsStream, stopTimesStream)
                val routeDetailsMap = parser.getRouteDetailsMap(stopsStream, tripsStream, stopTimesStream)

                // Helper to compute distance
                fun isNear(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Boolean {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
                    return results[0] <= radiusMeters
                }

                // Determine which routes have stops near both origin and destination (if set)
                val matchingRouteIds = routeStopsMap.filter { (_, stops) ->
                    val originSet = originLat != 0.0 || originLng != 0.0
                    val destSet = destinationLat != 0.0 || destinationLng != 0.0

                    // if neither is set, accept all
                    if (!originSet && !destSet) return@filter true

                    val originIndices = if (!originSet) emptyList() else stops.mapIndexedNotNull { idx, s -> if (isNear(originLat, originLng, s.latitude, s.longitude)) idx else null }
                    val destIndices = if (!destSet) emptyList() else stops.mapIndexedNotNull { idx, s -> if (isNear(destinationLat, destinationLng, s.latitude, s.longitude)) idx else null }

                    if (!originSet) return@filter destIndices.isNotEmpty()
                    if (!destSet) return@filter originIndices.isNotEmpty()

                    // Require at least one origin-stop that appears before at least one destination-stop on the route
                    originIndices.any { o -> destIndices.any { d -> o < d } }
                }.keys

                if (matchingRouteIds.isNotEmpty()) {
                    routesToDisplay = matchingRouteIds.map { routeId ->
                        val details = routeDetailsMap[routeId] ?: "Route $routeId"
                        Route(details, "Regular Operating Hours", "Est. 30-45 mins", "₱15.00 - ₱40.00")
                    }
                }
            } catch (e: Exception) {
                // If GTFS raw files missing or parsing fails, fall back to simple filtering by text
                android.util.Log.w("MainActivity", "GTFS spatial filtering unavailable: ${e.localizedMessage}")
            }

            // If spatial filtering yielded nothing, fall back to original text-based heuristic or dummy data
            if (routesToDisplay.isEmpty()) {
                val filteredRoutes = if (destinationName.isNotBlank()) {
                    gtfsRoutes.filter { route ->
                        route.details.contains(destinationName, ignoreCase = true) ||
                                route.details.contains("MRT", ignoreCase = true)
                    }
                } else {
                    gtfsRoutes
                }

                routesToDisplay = if (filteredRoutes.isNotEmpty()) {
                    filteredRoutes
                } else {
                    listOf(Route("No specific route found for $destinationName", "N/A", "N/A", "N/A"))
                }
            }

            withContext(Dispatchers.Main) {
                val adapter = RouteAdapter(routesToDisplay)
                adapter.setOnItemClickListener { route ->
                    showRouteDetailsPopup(route)
                }
                binding.rvRoutes.adapter = adapter
            }
        }

        val firebaseRef = FirebaseDatabase.getInstance().getReference("routes")
        firebaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    remoteRoutesList.clear()
                    for (routeSnapshot in snapshot.children) {
                        val details = routeSnapshot.child("details").getValue(String::class.java) ?: ""
                        val timeWindow = routeSnapshot.child("timeWindow").getValue(String::class.java) ?: ""
                        val duration = routeSnapshot.child("duration").getValue(String::class.java) ?: ""
                        val fare = routeSnapshot.child("fare").getValue(String::class.java) ?: ""

                        val route = Route(details, timeWindow, duration, fare)
                        remoteRoutesList.add(route)
                    }
                    if (remoteRoutesList.isNotEmpty()) {
                        val adapter = RouteAdapter(remoteRoutesList)
                        adapter.setOnItemClickListener { route ->
                            showRouteDetailsPopup(route)
                        }
                        binding.rvRoutes.adapter = adapter

                        lifecycleScope.launch(Dispatchers.IO) {
                            database.routeDao().clearAll()
                            database.routeDao().insertAll(remoteRoutesList)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load live Firebase data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Parse GTFS raw file (res/raw/routes.txt) safely off main thread
    private fun parseGtfsRoutesFromRaw(): List<Route> {
        val parsedRoutes = mutableListOf<Route>()
        val parser = GtfsParser()
        try {
            val routeDetailsMap = parser.getRouteDetailsMap(
                resources.openRawResource(R.raw.stops),
                resources.openRawResource(R.raw.trips),
                resources.openRawResource(R.raw.stop_times)
            )
            // Check for res/raw/routes.txt or res/raw/routes.csv
            val rawResourceId = resources.getIdentifier("routes", "raw", packageName)
            if (rawResourceId != 0) {
                val inputStream: InputStream = resources.openRawResource(rawResourceId)
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = parser.splitCsv(line)
                        if (tokens.size >= 10) {
                            val shortName = tokens[1]
                            val longName = tokens[2]

                            //val shortName = tokens[1].replace("\"", "").trim()
                            //val longName = tokens[2].replace("\"", "").trim()
                            val routeId = tokens[9]
                            val detailedStops = routeDetailsMap[routeId]

                            val details = if (!detailedStops.isNullOrBlank()) {
                                if (shortName.isNotBlank()) "($shortName) $detailedStops" else detailedStops
                            } else  {
                                if (shortName.isNotBlank()) "$shortName - $longName" else longName
                            }
                            //val details = if (shortName.isNotBlank()) "$shortName - $longName" else longName
                            //val timeWindow = "Regular Operating Hours"
                            //val duration = "Est. 30-45 mins"
                            //val fare = "₱15.00 - ₱40.00"
                            val distanceKm = calculateDistance(originLat, originLng, destinationLat, destinationLng)

                            val estimatedMinutes = (distanceKm / 20 * 60).toInt() + 10
                            val duration = if (distanceKm > 0) "Est. $estimatedMinutes mins" else "N/A"
                            val estimatedFare = 13.0 + (distanceKm * 2.0)
                            val fare = if (distanceKm > 0) String.format("₱%.2f", estimatedFare) else "N/A"

                            val timeWindow = "Next trip in ~15 mins"
                            parsedRoutes.add(Route(details, timeWindow, duration, fare))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return parsedRoutes
    }

    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        mMap.isMyLocationEnabled = true

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->

            if (location != null) {

                currentLat = location.latitude
                currentLng = location.longitude

                originLat = location.latitude
                originLng = location.longitude

                val currentLocation = LatLng(
                    currentLat,
                    currentLng
                )

                binding.tvFromAddress.text = "Current Location"

                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        currentLocation,
                        16f
                    )
                )
            }
        }
    }

    private fun showDestinationDialog() {

        val editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Enter Destination")
            .setMessage("Where would you like to go?")
            .setView(editText)

            .setPositiveButton("Search") { _, _ ->

                val destination = editText.text.toString().trim()

                if (destination.isNotEmpty()) {
                    updateDestination(destination)
                }

            }

            .setNegativeButton("Cancel", null)

            .show()
    }

    private fun updateDestination(destination: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
            try {

                val results = geocoder.getFromLocationName(destination, 1)
                withContext(Dispatchers.Main) {
                    if (!results.isNullOrEmpty()) {

                        val address = results[0]

                        val destinationLocation = LatLng(
                            address.latitude,
                            address.longitude
                        )
                        destinationName = destination
                        destinationLat = address.latitude
                        destinationLng = address.longitude

                        binding.tvToAddress.text = destination
                        destinationMarker?.remove()
                        destinationMarker = mMap.addMarker(
                            MarkerOptions()
                                .position(destinationLocation)
                                .title(destination)
                        )
                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(destinationLocation, 15f)
                        )
                        fetchRecommendedRoutes()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Destination not found.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {

                Toast.makeText(
                    this@MainActivity,
                    "Error finding destination.",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }

    private fun showOriginDialog() {
        val editText = EditText(this)
        editText.hint = "Enter origin or leave blank for Current Location"

        AlertDialog.Builder(this)
            .setTitle("Set Origin")
            .setMessage("Where are you starting from?")
            .setView(editText)
            .setPositiveButton("Set") { _, _ ->
                val input = editText.text.toString().trim()
                if (input.isEmpty()) {
                    resetToCurrentLocation()
                } else {
                    updateOrigin(input)
                }
            }
            .setNeutralButton("Use Current Location") { _, _ ->
                resetToCurrentLocation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetToCurrentLocation() {
        // Re-fetch current GPS coordinates
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        originLat = location.latitude
                        originLng = location.longitude
                        originName = "Current Location"
                        binding.tvFromAddress.text = originName

                        // This is just to update map marker (pin location design in the map) if needed
                        // mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(originLat, originLng), 15f))
                    }
                }
        }
    }

    private fun updateOrigin(addressName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Check Geocoder availability
                if (!Geocoder.isPresent()) {
                    Log.e("MainActivity", "Geocoder service is not present on this device")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Geocoding service unavailable on this device.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Use Activity context explicitly; do network/IO work on IO dispatcher
                val geocoder = Geocoder(this@MainActivity, Locale.getDefault())

                val results = try {
                    geocoder.getFromLocationName(addressName, 1)
                } catch (ioe: java.io.IOException) {
                    Log.e("MainActivity", "Geocoder IO error", ioe)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Network error during geocoding: ${ioe.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                if (!results.isNullOrEmpty()) {
                    val address = results[0]
                    originName = addressName
                    originLat = address.latitude
                    originLng = address.longitude

                    val originLatLng = LatLng(originLat, originLng)

                    // Switch to Main dispatcher for UI updates
                    withContext(Dispatchers.Main) {
                        binding.tvFromAddress.text = addressName

                        mMap.addMarker(
                            MarkerOptions().position(originLatLng).title("Start: $addressName")
                        )
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(originLatLng, 15f))

                        // Re-fetch routes based on new origin if destination is already set
                        if (destinationName.isNotEmpty()) {
                            fetchRecommendedRoutes()
                        }
                    }
                } else {
                    Log.i("MainActivity", "Geocoder returned no results for: $addressName")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Origin not found.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Unexpected error in updateOrigin", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error finding origin: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return (results[0] / 1000).toDouble() // Convert meters to KM
    }
}