package com.mobdeve.x21a.manatad.francinne.lakbay

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLngBounds
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivityMainBinding

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private val remoteRoutesList = mutableListOf<Route>()

    private lateinit var mMap: GoogleMap
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

        // Local Room route fallback
        lifecycleScope.launch(Dispatchers.IO) {
            val routeDao = database.routeDao()
            var localRoutes = routeDao.getAllRoutes()

            if (localRoutes.isEmpty()) {
                val dummyData = listOf(
                    Route("🚶‍♂️ 2 > 🚌 5 > 🚇 MRT-3 35", "10:00 AM - 11:00 AM", "1 hr", "₱40.00"),
                    Route("🚌 12 > 🚶‍♂️ 5 > 🚇 LRT-1 20", "10:15 AM - 11:15 AM", "55 mins", "₱35.00"),
                    Route("🚶‍♂️ 10 > 🚌 25", "10:30 AM - 11:30 AM", "1 hr 10 mins", "₱20.00"),
                    Route("🚕 Grab/Joyride", "Available Now", "25 mins", "₱180.00")
                )
                routeDao.insertAll(dummyData)
                localRoutes = routeDao.getAllRoutes()
            }

            withContext(Dispatchers.Main) {
                binding.rvRoutes.adapter = RouteAdapter(localRoutes)
            }
        }

        // Synchronize and fetch real-time route data from Firebase Database
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
                        binding.rvRoutes.adapter = RouteAdapter(remoteRoutesList)

                        // Persist to Room
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

        binding.cvSearch.setOnClickListener {
            val intent = Intent(this, CommuterActiveTripActivity::class.java)
            startActivity(intent)
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

                val currentLocation = LatLng(
                    location.latitude,
                    location.longitude
                )

                binding.tvFromAddress.text = "Current Location"

                // Temporary destination
                val cityHall = LatLng(14.5906, 120.9817)

                mMap.addMarker(
                    MarkerOptions()
                        .position(cityHall)
                        .title("Manila City Hall")
                )

                val bounds = LatLngBounds.Builder()
                    .include(currentLocation)
                    .include(cityHall)
                    .build()

                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, 100)
                )
            }
        }
    }
}