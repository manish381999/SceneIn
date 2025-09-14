package com.scenein.tickets.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.databinding.ItemTicketCardBinding
import com.scenein.tickets.data.models.Ticket
import com.scenein.utils.DateTimeUtils

class BrowseTicketsAdapter(private val onTicketClick: (Ticket) -> Unit) :
    ListAdapter<Ticket, BrowseTicketsAdapter.TicketViewHolder>(TicketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TicketViewHolder(private val binding: ItemTicketCardBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onTicketClick(getItem(adapterPosition))
                }
            }
        }

        fun bind(ticket: Ticket) {
            binding.tvEventName.text = ticket.eventName
            binding.tvEventVenue.text = ticket.eventVenue
            binding.tvSellerName.text = "Listed by ${ticket.sellerName ?: "VibeIn User"}"
            binding.tvSellingPrice.text = "₹${ticket.sellingPrice.substringBefore(".")}"
            binding.tvEventCategory.text = ticket.category_name?.uppercase() ?: "EVENT"

            // ✅ Format event date & time using DateTimeUtils

            binding.tvEventDate.text =
                DateTimeUtils.combineDateAndTime(ticket.eventDate, ticket.eventTime)
            // ✅ Load profile picture
            Glide.with(itemView.context)
                .load(ticket.sellerProfilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.ivSellerProfilePic)
        }
    }
}

class TicketDiffCallback : DiffUtil.ItemCallback<Ticket>() {
    override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket) = oldItem == newItem
}
