package com.mobdeve.x21a.manatad.francinne.lakbay

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

class CommuterActiveTripActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCommuterActiveTripBinding

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Define BroadcastReceiver to receive updates from background service
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
        val details = intentData.getStringExtra("ROUTE_DETAILS")
        val timeWindow = intentData.getStringExtra("ROUTE_TIME_WINDOW")
        val duration = intentData.getStringExtra("ROUTE_DURATION")

        binding.tvRouteDetails.text = details ?: "Jeepney (Taft Avenue)"
        binding.tvTimeWindow.text = timeWindow ?: "Quirino Avenue"
        binding.tvDuration.text = duration ?: "8 mins"

            // Start background location and trip tracking service
        val serviceIntent = Intent(this, TripTrackingService::class.java)
        startService(serviceIntent)

        binding.endTripBtn.setOnClickListener {
            // Stop background tracking service when trip ends
            stopService(serviceIntent)
            finish()
        }
    }

    // Unregister BroadcastReceiver in onStop() to avoid leaks
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
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->

                android.util.Log.d("GPS", "Location = $location")

                if (location != null) {

                    val currentLocation = LatLng(location.latitude, location.longitude)

                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            currentLocation,
                            16f
                        )
                    )

                } else {

                    // Fallback if location isn't available yet
                    val dlsu = LatLng(14.5648, 120.9936)

                    mMap.addMarker(
                        MarkerOptions()
                            .position(dlsu)
                            .title("DLSU Manila")
                    )

                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(dlsu, 16f)
                    )
                }
            }
        }
    }
}