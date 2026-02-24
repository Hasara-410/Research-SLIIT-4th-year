package com.example.qrscanfinalv2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProgressAdapter(
    private var items: List<ProgressPlaceItem>
) : RecyclerView.Adapter<ProgressAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPlaceName: TextView = itemView.findViewById(R.id.tvPlaceName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_progress_place, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvPlaceName.text = item.placeName
        holder.tvStatus.text = if (item.visited) "✅" else "❌"
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<ProgressPlaceItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
