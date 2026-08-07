package com.mobdeve.x21a.manatad.francinne.lakbay

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivityLocationSearchBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale

class LocationSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationSearchBinding
    private lateinit var adapter: LocationSuggestionAdapter
    private var cachedGtfsStops: List<GtfsStop> = emptyList()

    companion object {
        const val EXTRA_SELECTED_LOCATION = "EXTRA_SELECTED_LOCATION"
        const val EXTRA_SEARCH_MODE = "EXTRA_SEARCH_MODE" // "ORIGIN" or "DESTINATION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mode = intent.getStringExtra(EXTRA_SEARCH_MODE) ?: "DESTINATION"
        binding.tvSearchHeaderTitle.text = if (mode == "ORIGIN") "Start Location" else "Destination"

        binding.rvLocationSuggestions.layoutManager = LinearLayoutManager(this)
        adapter = LocationSuggestionAdapter(emptyList())
        binding.rvLocationSuggestions.adapter = adapter

        adapter.setOnItemClickListener { suggestion ->
            val resultIntent = Intent().apply {
                putExtra(EXTRA_SELECTED_LOCATION, suggestion.title)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        binding.ivClearQuery.setOnClickListener {
            binding.etSearchQuery.setText("")
        }

        binding.etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    performCombinedLocationSearch(query)
                } else {
                    adapter.updateData(emptyList())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        preloadGtfsStops()
    }

    private fun preloadGtfsStops() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val stopsId = resources.getIdentifier("stops", "raw", packageName)
                if (stopsId != 0) {
                    val inputStream: InputStream = resources.openRawResource(stopsId)
                    val parser = GtfsParser()
                    cachedGtfsStops = parser.parseStops(inputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun performCombinedLocationSearch(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val suggestionsList = mutableListOf<LocationSuggestion>()

            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(this@LocationSearchActivity, Locale("en", "PH"))

                    val geocodeResults = geocoder.getFromLocationName(
                        query,
                        10,
                        4.5,   // SW Latitude
                        116.0, // SW Longitude
                        21.0,  // NE Latitude
                        127.0  // NE Longitude
                    )

                    geocodeResults?.filter { address ->
                        address.countryCode.equals("PH", ignoreCase = true) ||
                                (address.countryName != null && address.countryName.contains("Philippines", ignoreCase = true))
                    }?.forEach { address ->
                        val title = address.featureName ?: address.locality ?: query
                        val subtitle = address.getAddressLine(0) ?: "${address.locality ?: ""}, Philippines"
                        suggestionsList.add(LocationSuggestion(title, subtitle))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val matchingGtfsStops = cachedGtfsStops.filter { stop ->
                stop.stopName.contains(query, ignoreCase = true)
            }.take(5)

            for (stop in matchingGtfsStops) {
                if (suggestionsList.none { it.title.equals(stop.stopName, ignoreCase = true) }) {
                    suggestionsList.add(
                        LocationSuggestion(
                            title = stop.stopName,
                            subtitle = "Public Transport Stop • Metro Manila"
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                adapter.updateData(suggestionsList)
            }
        }
    }
}