package com.mobdeve.x21a.manatad.francinne.lakbay

import android.Manifest
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

            intent.putExtra("CURRENT_LAT", currentLat)
            intent.putExtra("CURRENT_LNG", currentLng)

            intent.putExtra("ROUTE_TITLE", "Current Location → $destinationName")

            startActivity(intent)
        }

        binding.tvToAddress.setOnClickListener {
            showDestinationDialog()
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
                intent.putExtra("CURRENT_LAT", currentLat)
                intent.putExtra("CURRENT_LNG", currentLng)
                intent.putExtra("ROUTE_DETAILS", route.details)
                intent.putExtra("ROUTE_TIME_WINDOW", route.timeWindow)
                intent.putExtra("ROUTE_DURATION", route.duration)
                intent.putExtra("ROUTE_FARE", route.fare)
                intent.putExtra("ROUTE_TITLE", "Current Location → $destinationName")

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
            var gtfsRoutes = parseGtfsRoutesFromRaw()

            val routesToDisplay: List<Route> = if (gtfsRoutes.isNotEmpty()) {
                // Clear old dummy cache and update Room DB with parsed GTFS routes
                routeDao.clearAll()
                routeDao.insertAll(gtfsRoutes)
                gtfsRoutes
            } else {
                // Fallback to existing Room database entries or dummy data
                val localRoutes = routeDao.getAllRoutes()
                if (localRoutes.isEmpty()) {
                    val dummyData = listOf(
                        Route("🚶‍♂️ 2 > 🚌 5 > 🚇 MRT-3 35", "10:00 AM - 11:00 AM", "1 hr", "₱40.00"),
                        Route("🚌 12 > 🚶‍♂️ 5 > 🚇 LRT-1 20", "10:15 AM - 11:15 AM", "55 mins", "₱35.00"),
                        Route("🚶‍♂️ 10 > 🚌 25", "10:30 AM - 11:30 AM", "1 hr 10 mins", "₱20.00"),
                        Route("🚕 Grab/Joyride", "Available Now", "25 mins", "₱180.00")
                    )
                    routeDao.insertAll(dummyData)
                    routeDao.getAllRoutes()
                } else {
                    localRoutes
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
        try {
            // Check for res/raw/routes.txt or res/raw/routes.csv
            val rawResourceId = resources.getIdentifier("routes", "raw", packageName)
            if (rawResourceId != 0) {
                val inputStream: InputStream = resources.openRawResource(rawResourceId)
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",")
                        if (tokens.size >= 3) {
                            val shortName = tokens[1].replace("\"", "").trim()
                            val longName = tokens[2].replace("\"", "").trim()
                            val details = if (shortName.isNotBlank()) "$shortName - $longName" else longName
                            val timeWindow = "Regular Operating Hours"
                            val duration = "Est. 30-45 mins"
                            val fare = "₱15.00 - ₱40.00"
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

        val geocoder = Geocoder(this, Locale.getDefault())

        try {

            val results = geocoder.getFromLocationName(destination, 1)

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
                    this,
                    "Destination not found.",
                    Toast.LENGTH_SHORT
                ).show()

            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Error finding destination.",
                Toast.LENGTH_SHORT
            ).show()

        }

    }
}