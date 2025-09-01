package com.scenein.profile.persentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.scenein.R

class InterestAdapter(
    private val context: Context,
    private val interests: List<String>
) : RecyclerView.Adapter<InterestAdapter.InterestViewHolder>() {

    inner class InterestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvInterest: TextView = itemView.findViewById(R.id.tvInterest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_interest, parent, false)
        return InterestViewHolder(view)
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        holder.tvInterest.text = interests[position]
    }


    override fun getItemCount(): Int = interests.size
}
