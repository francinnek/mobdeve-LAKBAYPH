package com.mobdeve.x21a.manatad.francinne.lakbay

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
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
            val context = holder.itemView.context
            val intent = Intent(context, CommuterActiveTripActivity::class.java)

            intent.putExtra("ROUTE_DETAILS", route.details)
            intent.putExtra("ROUTE_TIME_WINDOW", route.timeWindow)
            intent.putExtra("ROUTE_DURATION", route.duration)
            intent.putExtra("ROUTE_FARE", route.fare)

            context.startActivity(intent)
        }

        holder.binding.ivReportRoute.setOnClickListener {
            val context = holder.itemView.context
            val editText = EditText(context)
            editText.hint = "Reason for report (e.g., outdated fare)"

            AlertDialog.Builder(context)
                .setTitle("Report Route")
                .setMessage("Please provide a reason for reporting this route:")
                .setView(editText)
                .setPositiveButton("Report") { _, _ ->
                    val reason = editText.text.toString().trim()
                    if (reason.isNotEmpty()) {
                        val report = mapOf(
                            "routeDetails" to route.details,
                            "reason" to reason,
                            "timestamp" to System.currentTimeMillis()
                        )
                        FirebaseDatabase.getInstance().getReference("route_reports")
                            .push().setValue(report)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Report submitted", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Please enter a reason", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount() = routes.size
}