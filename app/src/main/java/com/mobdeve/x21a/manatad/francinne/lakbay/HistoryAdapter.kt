package com.mobdeve.x21a.manatad.francinne.lakbay

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ItemHistoryBinding

class HistoryAdapter(
    private val historyList: List<CommuteHistory>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(history: CommuteHistory) {
            binding.tvHistoryRoute.text = "${history.origin} → ${history.destination}"
            binding.tvHistoryDate.text = history.date
            binding.tvHistoryDuration.text = "Duration: ${history.duration}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bindData(historyList[position])
    }

    override fun getItemCount(): Int = historyList.size
}