package com.mobdeve.x21a.manatad.francinne.lakbay

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivityCommuterActiveTripBinding
import java.util.concurrent.Executors

class CommuterActiveTripActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCommuterActiveTripBinding

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var destinationName = ""
    private var destinationLat = 0.0
    private var destinationLng = 0.0

    private var currentLat = 0.0
    private var currentLng = 0.0

    // Initialize ExecutorService for offloading GTFS processing
    private val executorService = Executors.newSingleThreadExecutor()

    private val tripReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TripTrackingService.ACTION_TRIP_UPDATE) {
                val elapsedSeconds = intent.getIntExtra(TripTrackingService.EXTRA_ELAPSED_SECONDS, 0)
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                binding.tvDuration.text = String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommuterActiveTripBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        val intentData = intent

        val routeTitle = intentData.getStringExtra("ROUTE_TITLE")

        destinationName = intentData.getStringExtra("DESTINATION_NAME") ?: ""

        destinationLat = intentData.getDoubleExtra("DESTINATION_LAT", 0.0)
        destinationLng = intentData.getDoubleExtra("DESTINATION_LNG", 0.0)

        currentLat = intentData.getDoubleExtra("CURRENT_LAT", 0.0)
        currentLng = intentData.getDoubleExtra("CURRENT_LNG", 0.0)

        val details = intentData.getStringExtra("ROUTE_DETAILS")
        val timeWindow = intentData.getStringExtra("ROUTE_TIME_WINDOW")
        val duration = intentData.getStringExtra("ROUTE_DURATION")

        binding.tvRouteDetails.text = details ?: "Jeepney (Taft Avenue)"
        binding.tvTimeWindow.text =
            if (destinationName.isNotBlank())
                destinationName
            else
                (timeWindow ?: "Destination")

        binding.tvTripRoute.text =
            routeTitle ?: "Current Location → Destination"

        binding.tvDuration.text = duration ?: "8 mins"

        val serviceIntent = Intent(this, TripTrackingService::class.java)
        startService(serviceIntent)

        binding.endTripBtn.setOnClickListener {
            stopService(serviceIntent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(TripTrackingService.ACTION_TRIP_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tripReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(tripReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(tripReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
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

        val currentLocation = LatLng(currentLat, currentLng)

        if (destinationLat != 0.0 && destinationLng != 0.0) {

            val destination = LatLng(
                destinationLat,
                destinationLng
            )

            mMap.addMarker(
                MarkerOptions()
                    .position(destination)
                    .title(destinationName)
            )

            val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                .include(currentLocation)
                .include(destination)
                .build()

            mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 200)
            )

        } else {

            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    currentLocation,
                    16f
                )
            )

        }

        // Load and parse GTFS stops on map off the main thread
        loadGtfsStops()
    }

    private fun loadGtfsStops() {
        executorService.execute {
            try {
                // Place stops.txt inside res/raw/stops.txt
                val inputStream = resources.openRawResource(R.raw.stops)
                val parser = GtfsParser()
                val stops = parser.parseStops(inputStream)

                // Update UI elements on main thread
                runOnUiThread {
                    for (stop in stops) {
                        val stopLatLng = LatLng(stop.latitude, stop.longitude)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(stopLatLng)
                                .title(stop.stopName)
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}