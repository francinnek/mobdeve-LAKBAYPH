package com.mobdeve.x21a.manatad.francinne.lakbay

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivityCommuterActiveTripBinding

class CommuterActiveTripActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommuterActiveTripBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommuterActiveTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intentData = intent
        val details = intentData.getStringExtra("ROUTE_DETAILS")
        val timeWindow = intentData.getStringExtra("ROUTE_TIME_WINDOW")
        val duration = intentData.getStringExtra("ROUTE_DURATION")

        binding.tvRouteDetails.text = details ?: "Jeepney (Taft Avenue)"
        binding.tvTimeWindow.text = timeWindow ?: "Quirino Avenue"
        binding.tvDuration.text = duration ?: "8 mins"

        binding.endTripBtn.setOnClickListener {
            finish()
        }
    }
}