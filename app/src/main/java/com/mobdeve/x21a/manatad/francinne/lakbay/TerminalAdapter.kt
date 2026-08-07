package com.mobdeve.x21a.manatad.francinne.lakbay

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ItemTerminalBinding

class TerminalAdapter(
    private var terminalList: List<Terminal>
) : RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder>() {

    private var onReportClickListener: ((Terminal) -> Unit)? = null

    fun setOnReportClickListener(listener: (Terminal) -> Unit) {
        onReportClickListener = listener
    }

    fun updateData(newTerminals: List<Terminal>) {
        terminalList = newTerminals
        notifyDataSetChanged()
    }

    class TerminalViewHolder(val binding: ItemTerminalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(terminal: Terminal) {
            binding.tvTerminalName.text = terminal.name
            binding.tvCongestionLevel.text = "${terminal.congestion}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TerminalViewHolder {
        val binding = ItemTerminalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TerminalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TerminalViewHolder, position: Int) {
        val terminal = terminalList[position]
        holder.bindData(terminal)

        holder.binding.btnReport.setOnClickListener {
            onReportClickListener?.invoke(terminal)
        }
    }

    override fun getItemCount(): Int = terminalList.size
}