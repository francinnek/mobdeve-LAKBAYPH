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

        binding.endTripBtn.setOnClickListener {
            finish()
        }
    }
}
