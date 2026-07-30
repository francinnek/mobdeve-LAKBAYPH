package com.mobdeve.x21a.manatad.francinne.lakbay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvRoutes.layoutManager = LinearLayoutManager(this)

        // Initialize SQLite helper instance
        dbHelper = DatabaseHelper.getInstance(this)

        // Retrieve stored routes from SQLite DB
        var routesList = dbHelper.getAllRoutes()

        // Populate database with default routes if empty
        if (routesList.isEmpty()) {
            val initialRoutes = listOf(
                Route("🚶‍♂️ 2 > 🚌 5 > 🚇 MRT-3 35", "10:00 AM - 11:00 AM", "1 hr", "₱40.00"),
                Route("🚌 12 > 🚶‍♂️ 5 > 🚇 LRT-1 20", "10:15 AM - 11:15 AM", "55 mins", "₱35.00"),
                Route("🚶‍♂️ 10 > 🚌 25", "10:30 AM - 11:30 AM", "1 hr 10 mins", "₱20.00"),
                Route("🚕 Grab/Joyride", "Available Now", "25 mins", "₱180.00")
            )
            for (route in initialRoutes) {
                dbHelper.insertRoute(route)
            }
            routesList = dbHelper.getAllRoutes()
        }

        binding.rvRoutes.adapter = RouteAdapter(routesList)

        binding.cvSearch.setOnClickListener {
            val intent = Intent(this, CommuterActiveTripActivity::class.java)
            startActivity(intent)
        }
    }
}