package com.mobdeve.x21a.manatad.francinne.lakbay

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivityCommuterActiveTripBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    private val executorService = Executors.newSingleThreadExecutor()

    private val tripReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TripTrackingService.ACTION_TRIP_UPDATE) {
                val elapsedSeconds = intent.getIntExtra(TripTrackingService.EXTRA_ELAPSED_SECONDS, 0)
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                binding.tvDuration.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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

            val finalDuration = binding.tvDuration.text.toString()
            val finalOrigin = routeTitle?.substringBefore(" → ") ?: "Current Location"
            val finalDestination = destinationName.ifBlank { "Destination" }
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val currentDate = sdf.format(Date())

            val historyEntry = CommuteHistory(
                origin = finalOrigin,
                destination = finalDestination,
                date = currentDate,
                duration = finalDuration
            )

            executorService.execute {
                AppDatabase.getDatabase(this@CommuterActiveTripActivity)
                    .historyDao()
                    .insertHistory(historyEntry)

                runOnUiThread {
                    finish()
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(TripTrackingService.ACTION_TRIP_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tripReceiver, filter, RECEIVER_NOT_EXPORTED)
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

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdown()
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
        val originName = intent.getStringExtra("ROUTE_TITLE")?.substringBefore(" → ") ?: "Current Location"
        mMap.addMarker(
            MarkerOptions()
                .position(currentLocation)
                .title(originName)
        )

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

            val mapFragmentForLayout = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            val mapView = mapFragmentForLayout?.view
            if (mapView != null) {
                mapView.post {
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds, 200)
                    )
                }
            } else {
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, 200)
                )
            }

        } else {

            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    currentLocation,
                    16f
                )
            )

        }

        val routeId = intent.getStringExtra("ROUTE_ID")
        if (routeId != null) {
            executorService.execute {
                try {
                    val parser = GtfsParser()

                    val shapeIds = parser.getAllShapeIdsForRoute(resources.openRawResource(R.raw.trips), routeId)

                    var bestTripSegment = listOf<LatLng>()
                    var minTotalError = Double.MAX_VALUE

                    for (sId in shapeIds) {
                        val points = parser.getShapePoints(resources.openRawResource(R.raw.shapes), sId)
                        if (points.isEmpty()) continue

                        val startIdx = findClosestPointIndex(points, currentLat, currentLng)
                        val endIdx = findClosestPointIndex(points, destinationLat, destinationLng)

                        val dStart = distanceInMeters(currentLat, currentLng, points[startIdx].latitude, points[startIdx].longitude)
                        val dEnd = distanceInMeters(destinationLat, destinationLng, points[endIdx].latitude, points[endIdx].longitude)

                        if ((dStart + dEnd) < minTotalError) {
                            minTotalError = (dStart + dEnd).toDouble()
                            val start = Math.min(startIdx, endIdx)
                            val end = Math.max(startIdx, endIdx)
                            bestTripSegment = points.subList(start, end + 1)
                        }
                    }

                    if (bestTripSegment.isNotEmpty()) {
                        runOnUiThread {
                            mMap.addPolyline(
                                PolylineOptions()
                                    .addAll(bestTripSegment)
                                    .width(20f)
                                    .color(android.graphics.Color.BLUE)
                                    .geodesic(true)
                            )
                        }
                    } else {
                        drawRouteFromStops(routeId, parser)
                    }
                } catch (e: Exception) {
                    Log.e("LAKBAY_POLYLINE", "Error loading polyline: ${e.message}", e)
                }
            }
        }
    }
 
    private fun drawRouteFromStops(routeId: String, parser: GtfsParser) {
        try {
            val context = this@CommuterActiveTripActivity
            val routeStopsMap = parser.getRouteStopsMap(
                context.resources.openRawResource(context.resources.getIdentifier("stops", "raw", context.packageName)),
                context.resources.openRawResource(context.resources.getIdentifier("trips", "raw", context.packageName)),
                context.resources.openRawResource(context.resources.getIdentifier("stop_times", "raw", context.packageName))
            )

            val stopsForRoute = routeStopsMap[routeId] ?: emptyList()
            Log.d("LAKBAY_POLYLINE", "Stops for route $routeId: ${stopsForRoute.size}")

            if (stopsForRoute.isNotEmpty()) {
                // Find the closest stop to origin
                var closestOriginIndex = 0
                var minOriginDistance = Double.MAX_VALUE
                stopsForRoute.forEachIndexed { index, stop ->
                    val distance = distanceInMeters(currentLat, currentLng, stop.latitude, stop.longitude)
                    if (distance < minOriginDistance) {
                        minOriginDistance = distance.toDouble()
                        closestOriginIndex = index
                    }
                }

                // Find the closest stop to destination
                var closestDestIndex = stopsForRoute.size - 1
                var minDestDistance = Double.MAX_VALUE
                stopsForRoute.forEachIndexed { index, stop ->
                    val distance = distanceInMeters(destinationLat, destinationLng, stop.latitude, stop.longitude)
                    if (distance < minDestDistance) {
                        minDestDistance = distance.toDouble()
                        closestDestIndex = index
                    }
                }

                Log.d("LAKBAY_POLYLINE", "Closest stops - Origin: $closestOriginIndex, Destination: $closestDestIndex (out of ${stopsForRoute.size})")
                val start = Math.min(closestOriginIndex, closestDestIndex)
                val end = Math.max(closestOriginIndex, closestDestIndex)
                val points = stopsForRoute.subList(start, end + 1).map { LatLng(it.latitude, it.longitude) }
                
                if (points.isNotEmpty()) {
                    context.runOnUiThread {
                        mMap.addPolyline(
                            PolylineOptions()
                                .addAll(points)
                                .width(20f)
                                .color(android.graphics.Color.BLUE)
                                .geodesic(true)
                        )
                        Log.d("LAKBAY_POLYLINE", "Polyline added successfully from ${points.size} stops")
                    }
                }
            } else {
                Log.w("LAKBAY_POLYLINE", "No stops found for route: $routeId")
            }
        } catch (e: Exception) {
            Log.e("LAKBAY_POLYLINE", "Error drawing route from stops: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun loadGtfsStops() {
        executorService.execute {
            try {
                val stopsRawId = resources.getIdentifier("stops", "raw", packageName)
                if (stopsRawId != 0) {
                    val inputStream = resources.openRawResource(stopsRawId)
                    val parser = GtfsParser()
                    val stops = parser.parseStops(inputStream)

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
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    // Helper fun for calculating the EST and fare
    private fun distanceInMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    // Helper fun to help look for the closest stops for origin and destination
    private fun findClosestPointIndex(points: List<LatLng>, targetLat: Double, targetLng: Double): Int {
        var closestIndex = 0
        var minDistance = Float.MAX_VALUE
        points.forEachIndexed { index, point ->
            val distance = distanceInMeters(targetLat, targetLng, point.latitude, point.longitude)
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = index
            }
        }
        return closestIndex
    }

}