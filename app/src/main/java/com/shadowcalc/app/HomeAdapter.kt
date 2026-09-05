package com.shadowcalc.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HomeAdapter(private val items: List<HomeItem>) : RecyclerView.Adapter<HomeAdapter.VH>() {
    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.ivIcon)
        val name: TextView = v.findViewById(R.id.tvName)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_home, p, false))
    override fun onBindViewHolder(h: VH, i: Int) {
        val item = items[i]
        h.name.text = item.name
        h.icon.setImageResource(item.iconRes)
        h.itemView.setOnClickListener { item.action() }
    }
    override fun getItemCount() = items.size
}
