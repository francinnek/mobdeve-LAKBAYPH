package com.mobdeve.x21a.manatad.francinne.lakbay

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

    private var currentLat = 0.0
    private var currentLng = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var originName = "Current Location"
    private var originLat = 0.0
    private var originLng = 0.0

    private val destinationSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedLocation = result.data?.getStringExtra(LocationSearchActivity.EXTRA_SELECTED_LOCATION)
            if (!selectedLocation.isNullOrBlank()) {
                updateDestination(selectedLocation)
            }
        }
    }

    private val originSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedLocation = result.data?.getStringExtra(LocationSearchActivity.EXTRA_SELECTED_LOCATION)
            if (!selectedLocation.isNullOrBlank()) {
                updateOrigin(selectedLocation)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.clRoutesPanel.visibility = View.GONE

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        binding.rvRoutes.layoutManager = LinearLayoutManager(this)

        database = AppDatabase.getDatabase(this)

        binding.navNavigate.setOnClickListener {
            // Already in Navigate view; no action needed.
        }

        binding.navTerminals.setOnClickListener {
            val intent = Intent(this, TerminalsActivity::class.java)
            startActivity(intent)
        }

        binding.navHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        binding.navSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

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
            intent.putExtra("CURRENT_LAT", originLat)
            intent.putExtra("CURRENT_LNG", originLng)
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
                intent.putExtra("CURRENT_LAT", originLat)
                intent.putExtra("CURRENT_LNG", originLng)

                intent.putExtra("ROUTE_DETAILS", route.details)
                intent.putExtra("ROUTE_TIME_WINDOW", route.timeWindow)
                intent.putExtra("ROUTE_DURATION", route.duration)
                intent.putExtra("ROUTE_FARE", route.fare)
                intent.putExtra("ROUTE_TITLE", "$originName → $destinationName")

                intent.putExtra("ROUTE_ID", route.routeId)

                startActivity(intent)
            }
            .setNegativeButton("Choose Another", null)
            .show()
    }

    private fun fetchRecommendedRoutes() {
        // Clear current list to show user that search is in progress
        binding.rvRoutes.adapter = RouteAdapter(emptyList())

        lifecycleScope.launch(Dispatchers.IO) {
            val routeDao = database.routeDao()
            val gtfsRoutes = parseGtfsRoutesFromRaw()

            // Also fetch from Firebase once to see if there are additional relevant routes
            val firebaseRoutes = fetchFirebaseRoutesSynchronously()
            // In a real app, Firebase routes would also be filtered by location.
            // For now, we combine them, but GTFS routes are already spatially filtered.
            val allRoutes = (gtfsRoutes + firebaseRoutes).distinctBy { it.details }

            if (allRoutes.isNotEmpty()) {
                routeDao.clearAll()
                routeDao.insertAll(allRoutes)
            }

            withContext(Dispatchers.Main) {
                if (allRoutes.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No routes found for this trip.", Toast.LENGTH_SHORT).show()
                }
                val adapter = RouteAdapter(allRoutes)
                adapter.setOnItemClickListener { route ->
                    showRouteDetailsPopup(route)
                }
                binding.rvRoutes.adapter = adapter
            }
        }
    }

    private suspend fun fetchFirebaseRoutesSynchronously(): List<Route> = withContext(Dispatchers.IO) {
        val routes = mutableListOf<Route>()
        try {
            val snapshot = com.google.android.gms.tasks.Tasks.await(
                FirebaseDatabase.getInstance().getReference("routes").get()
            )
            if (snapshot.exists()) {
                for (routeSnapshot in snapshot.children) {
                    val details = routeSnapshot.child("details").getValue(String::class.java) ?: ""
                    val timeWindow = routeSnapshot.child("timeWindow").getValue(String::class.java) ?: ""
                    val duration = routeSnapshot.child("duration").getValue(String::class.java) ?: ""
                    val fare = routeSnapshot.child("fare").getValue(String::class.java) ?: ""
                    val routeId = routeSnapshot.child("route_id").getValue(String::class.java) ?: routeSnapshot.key

                    routes.add(Route(details, timeWindow, duration, fare, routeId))
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase fetch failed", e)
        }
        routes
    }

    private fun parseGtfsRoutesFromRaw(): List<Route> {
        val parsedRoutes = mutableListOf<Route>()
        val parser = GtfsParser()

        val routeStopsMap = parser.getRouteStopsMap(
            resources.openRawResource(
                resources.getIdentifier("stops", "raw", packageName)
            ),
            resources.openRawResource(
                resources.getIdentifier("trips", "raw", packageName)
            ),
            resources.openRawResource(
                resources.getIdentifier("stop_times", "raw", packageName)
            )
        )

        try {
            val rawResourceId = resources.getIdentifier("routes", "raw", packageName)
            if (rawResourceId != 0) {
                val inputStream: InputStream = resources.openRawResource(rawResourceId)
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = parser.splitCsv(line)
                        if (tokens.size >= 10) {

                            val routeId = tokens[9].trim()
                            val stopsForRoute = routeStopsMap[routeId] ?: emptyList()

                            val nearOrigin = routePassesNearLocation(stopsForRoute, originLat, originLng)
                            val nearDestination = routePassesNearLocation(stopsForRoute, destinationLat, destinationLng)

                            if (!nearOrigin || !nearDestination) { return@forEach }

                            val shortName = tokens[1].replace("\"", "").trim()
                            val longName = tokens[2].replace("\"", "").trim()
                            val details = if (shortName.isNotBlank()) "$shortName - $longName" else longName
                            val timeWindow = "Regular Operating Hours"
                            
                            // Calculating EST and estimated fare
                            val distanceMeters = distanceInMeters(originLat, originLng, destinationLat, destinationLng)
                            val distanceKm = distanceMeters / 1000f
                            
                            // Estimate time: average speed ~20 km/h in Manila traffic
                            val avgSpeed = 20f
                            val estimatedMins = ((distanceKm / avgSpeed) * 60).toInt()
                            val duration = if (estimatedMins > 0) "Est. $estimatedMins mins" else "Est. 5-10 mins"
                            
                            // Calculate fare: ₱10 base + ₱1.5 per km
                            val baseFare = 10f
                            val perKmFare = 1.5f
                            val totalFare = baseFare + (distanceKm * perKmFare)
                            val fare = "₱${"%.2f".format(totalFare)}"
                            
                            parsedRoutes.add(Route(details, timeWindow, duration, fare, routeId))
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
        val intent = Intent(this, LocationSearchActivity::class.java).apply {
            putExtra(LocationSearchActivity.EXTRA_SEARCH_MODE, "DESTINATION")
        }
        destinationSearchLauncher.launch(intent)
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

                        binding.clRoutesPanel.visibility = View.VISIBLE
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
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error finding destination.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun showOriginDialog() {
        val intent = Intent(this, LocationSearchActivity::class.java).apply {
            putExtra(LocationSearchActivity.EXTRA_SEARCH_MODE, "ORIGIN")
        }
        originSearchLauncher.launch(intent)
    }

    private fun resetToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        originLat = location.latitude
                        originLng = location.longitude
                        originName = "Current Location"
                        binding.tvFromAddress.text = originName
                    }
                }
        }
    }

    private fun updateOrigin(addressName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    Log.e("MainActivity", "Geocoder service is not present on this device")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Geocoding service unavailable on this device.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

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

                    withContext(Dispatchers.Main) {
                        binding.tvFromAddress.text = addressName

                        mMap.addMarker(
                            MarkerOptions().position(originLatLng).title("Start: $addressName")
                        )
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(originLatLng, 15f))

                        if (destinationName.isNotEmpty()) {
                            binding.clRoutesPanel.visibility = View.VISIBLE
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

    private fun distanceInMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {

        val results = FloatArray(1)

        android.location.Location.distanceBetween(
            lat1,
            lng1,
            lat2,
            lng2,
            results
        )

        return results[0]
    }

    private fun routePassesNearLocation(
        stops: List<GtfsStop>,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 1000f
    ): Boolean {

        return stops.any { stop ->

            distanceInMeters(
                latitude,
                longitude,
                stop.latitude,
                stop.longitude
            ) <= radiusMeters

        }

    }
}