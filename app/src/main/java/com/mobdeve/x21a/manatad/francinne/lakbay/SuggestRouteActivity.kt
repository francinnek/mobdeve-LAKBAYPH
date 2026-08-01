package com.mobdeve.x21a.manatad.francinne.lakbay

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.database.FirebaseDatabase
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivitySuggestRouteBinding

class SuggestRouteActivity : ComponentActivity() {

    private lateinit var binding: ActivitySuggestRouteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuggestRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSubmit.setOnClickListener {
            val details = binding.etRouteDetails.text.toString().trim()
            val timeWindow = binding.etTimeWindow.text.toString().trim()
            val duration = binding.etDuration.text.toString().trim()
            val fare = binding.etFare.text.toString().trim()

            if (details.isEmpty() || timeWindow.isEmpty() || duration.isEmpty() || fare.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val suggestion = mapOf(
                "details" to details,
                "timeWindow" to timeWindow,
                "duration" to duration,
                "fare" to fare,
                "timestamp" to System.currentTimeMillis()
            )

            val database = FirebaseDatabase.getInstance().getReference("community_suggestions")
            database.push().setValue(suggestion)
                .addOnSuccessListener {
                    Toast.makeText(this, "Suggestion submitted successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to submit suggestion: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}