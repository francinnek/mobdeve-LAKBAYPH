package com.mobdeve.x21a.manatad.francinne.lakbay

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ItemRouteBinding

class RouteAdapter(private val routes: List<Route>) : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    class RouteViewHolder(val binding: ItemRouteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]
        with(holder.binding) {
            tvRouteDetails.text = route.details
            tvTimeWindow.text = route.timeWindow
            tvDuration.text = route.duration
            tvFare.text = route.fare
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, CommuterActiveTripActivity::class.java)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = routes.size
}
