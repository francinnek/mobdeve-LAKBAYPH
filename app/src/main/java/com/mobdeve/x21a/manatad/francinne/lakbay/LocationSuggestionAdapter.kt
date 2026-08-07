package com.mobdeve.x21a.manatad.francinne.lakbay

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ItemLocationSuggestionBinding

class LocationSuggestionAdapter(
    private var locations: List<LocationSuggestion>
) : RecyclerView.Adapter<LocationSuggestionAdapter.LocationViewHolder>() {

    private var onItemClickListener: ((LocationSuggestion) -> Unit)? = null

    fun setOnItemClickListener(listener: (LocationSuggestion) -> Unit) {
        onItemClickListener = listener
    }

    fun updateData(newLocations: List<LocationSuggestion>) {
        locations = newLocations
        notifyDataSetChanged()
    }

    class LocationViewHolder(val binding: ItemLocationSuggestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindData(location: LocationSuggestion) {
            binding.tvLocationTitle.text = location.title
            binding.tvLocationSubtitle.text = location.subtitle
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locations[position]
        holder.bindData(location)

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(location)
        }
    }

    override fun getItemCount(): Int = locations.size
}