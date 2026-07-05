package com.mobdeve.x21a.mco.group8

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.x21a.mco.group8.databinding.ItemRouteBinding // Adjust to your actual package

class RouteAdapter(private val routeList: ArrayList<RouteModel>) :
    RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(routeList[position])
    }

    override fun getItemCount(): Int = routeList.size

    class RouteViewHolder(private val binding: ItemRouteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(route: RouteModel) {
            binding.tvRouteDetails.text = route.routeDescription
            binding.tvTimeWindow.text = route.timeAdvisory
            binding.tvDuration.text = route.duration
            binding.tvFare.text = route.fare
        }
    }
}