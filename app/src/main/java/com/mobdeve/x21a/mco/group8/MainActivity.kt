package com.mobdeve.x21a.mco.group8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mobdeve.x21a.mco.group8.ui.theme.LakbayTheme
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.x21a.mco.group8.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var routeAdapter: RouteAdapter
    private val sampleDataList = ArrayList<RouteModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        generateMockData()

        binding.rvRoutes.layoutManager = LinearLayoutManager(this)

        routeAdapter = RouteAdapter(sampleDataList)
        binding.rvRoutes.adapter = routeAdapter
    }

    private fun generateMockData() {
        sampleDataList.add(
            RouteModel(
                routeDescription = "🚶‍♂️ Walk to Vito Cruz Station (2 min) > 🚇 LRT-1 Train to Central Terminal (8 min) > 🚶‍♂️ Walk to Manila City Hall (3 min)",
                timeAdvisory = "From: De La Salle University (Taft Ave)\nTo: Manila City Hall\nLRT-1 operating hours apply.",
                duration = "13 min",
                fare = "₱20.00"
            )
        )

        sampleDataList.add(
            RouteModel(
                routeDescription = "🚶‍♂️ Walk to Taft Ave (1 min) > 🛺 Jeepney (Monumento / Divisoria route) to Manila City Hall / Lawton (15 min)",
                timeAdvisory = "From: De La Salle University (Taft Ave)\nTo: Manila City Hall\nSubject to Taft Ave traffic conditions.",
                duration = "16 min",
                fare = "₱15.00"
            )
        )

        sampleDataList.add(
            RouteModel(
                routeDescription = "🚶‍♂️ Walk straight down northbound sidewalk of Taft Avenue directly past UN Avenue",
                timeAdvisory = "Distance: ~3.3 km. Not recommended during midday heat or heavy rain.",
                duration = "40 min",
                fare = "FREE"
            )
        )

        if (::routeAdapter.isInitialized) {
            routeAdapter.notifyDataSetChanged()
        }
    }
}
