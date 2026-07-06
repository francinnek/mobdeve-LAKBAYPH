package com.mobdeve.x21a.manatad.francinne.lakbay

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ItemRouteBinding

class RouteAdapter(private val routes: List<Route>) : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    class RouteViewHolder(val binding: ItemRouteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(route: Route) {
            with(binding) {
                tvRouteDetails.text = route.details
                tvTimeWindow.text = route.timeWindow
                tvDuration.text = route.duration
                tvFare.text = route.fare
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]

        holder.bindData(route)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context // Rule of thumb: Use view/activity context for Views
            val intent = Intent(context, CommuterActiveTripActivity::class.java) //

            intent.putExtra("ROUTE_DETAILS", route.details)
            intent.putExtra("ROUTE_TIME_WINDOW", route.timeWindow)
            intent.putExtra("ROUTE_DURATION", route.duration)
            intent.putExtra("ROUTE_FARE", route.fare)

            context.startActivity(intent) //
        }
    }

    override fun getItemCount() = routes.size //
}