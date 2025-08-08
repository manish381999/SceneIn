package com.tie.vibein.profile.persentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tie.vibein.databinding.ItemProfileStatBinding
import com.tie.vibein.profile.data.models.StatItem

class StatsAdapter : ListAdapter<StatItem, StatsAdapter.StatViewHolder>(DiffCallback) {

    class StatViewHolder(private val binding: ItemProfileStatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StatItem) {
            binding.tvStatCount.text = item.count
            binding.tvStatLabel.text = item.label
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val binding = ItemProfileStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<StatItem>() {
            override fun areItemsTheSame(oldItem: StatItem, newItem: StatItem) = oldItem.label == newItem.label
            override fun areContentsTheSame(oldItem: StatItem, newItem: StatItem) = oldItem == newItem
        }
    }
}