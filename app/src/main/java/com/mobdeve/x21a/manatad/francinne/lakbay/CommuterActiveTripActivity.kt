package com.mobdeve.x21a.manatad.francinne.lakbay

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivityCommuterActiveTripBinding

class CommuterActiveTripActivity : ComponentActivity() {

    private lateinit var binding: ActivityCommuterActiveTripBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommuterActiveTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intentData = intent
        val details = intentData.getStringExtra("ROUTE_DETAILS")
        val timeWindow = intentData.getStringExtra("ROUTE_TIME_WINDOW")
        val duration = intentData.getStringExtra("ROUTE_DURATION")

        binding.tvRouteDetails.text = details
        binding.tvTimeWindow.text = timeWindow
        binding.tvDuration.text = duration

        binding.endTripBtn.setOnClickListener {
            finish()
        }
    }
}