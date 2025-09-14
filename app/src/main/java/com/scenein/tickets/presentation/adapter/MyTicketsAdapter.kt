package com.scenein.tickets.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scenein.R
import com.scenein.databinding.ItemMyTicketCardBinding
import com.scenein.tickets.data.models.Ticket
import com.scenein.utils.DateTimeUtils
import java.util.Locale

class MyTicketsAdapter(
    private val onTicketClick: (Ticket) -> Unit
) : ListAdapter<Ticket, MyTicketsAdapter.MyTicketViewHolder>(DiffCallback) {

    class MyTicketViewHolder(private val binding: ItemMyTicketCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: Ticket, onTicketClick: (Ticket) -> Unit) {
            val context = binding.root.context

            // --- 1. Populate Core Information ---
            binding.tvEventName.text = ticket.eventName
            binding.tvEventCategory.text = ticket.category_name?.uppercase(Locale.ROOT) ?: "EVENT"
            binding.tvPrice.text = "₹${ticket.sellingPrice.toDoubleOrNull()?.toInt() ?: 0}"

            // --- 2. Format Date & Time using DateTimeUtils ---
            val formattedDate = DateTimeUtils.formatEventDate(ticket.eventDate)
            val formattedTime = DateTimeUtils.formatEventTimeRange(ticket.eventTime, null)

            binding.tvEventDateTime.text = "$formattedDate, $formattedTime"

            // --- 3. Determine UI state (purchased vs. listed) ---
            val isPurchased = ticket.transactionId != null

            if (isPurchased) {
                binding.tvTicketTypeHeader.text = "PURCHASED TICKET"
                val (statusText, statusColorRes) = when {
                    ticket.completionType == "resold" -> "RESOLD BY YOU" to R.color.gray600
                    ticket.transactionStatus in listOf("refunded", "refund_processed") -> "REFUNDED" to R.color.textSecondary
                    ticket.transactionStatus in listOf("refund_queued", "refund_actioned") -> "REFUNDING" to R.color.colorWarning
                    ticket.transactionStatus == "escrow" -> "VALID FOR ENTRY" to R.color.colorSuccess
                    ticket.transactionStatus == "in_dispute" -> "IN DISPUTE" to R.color.colorWarning
                    ticket.transactionStatus in listOf("completed_by_user", "completed_auto", "payout_queued", "payout_processed") -> "USED" to R.color.textSecondary
                    else -> "PROCESSING" to R.color.gray400
                }
                binding.chipStatus.text = statusText
                binding.chipStatus.setChipBackgroundColorResource(statusColorRes)
                binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.white))

            } else {
                binding.tvTicketTypeHeader.text = "TICKET FOR SALE"
                binding.chipStatus.text = ticket.listingStatus.uppercase(Locale.ROOT)

                val statusColorRes = when (ticket.listingStatus.lowercase(Locale.ROOT)) {
                    "live" -> R.color.colorSuccess
                    "sold" -> R.color.colorWarning
                    "expired", "delisted" -> R.color.gray400
                    else -> R.color.gray200
                }
                binding.chipStatus.setChipBackgroundColorResource(statusColorRes)
                binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
            }

            itemView.setOnClickListener { onTicketClick(ticket) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyTicketViewHolder {
        val binding = ItemMyTicketCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyTicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyTicketViewHolder, position: Int) {
        holder.bind(getItem(position), onTicketClick)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Ticket>() {
            override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
                return oldItem.id == newItem.id && oldItem.transactionId == newItem.transactionId
            }
            override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
                return oldItem == newItem
            }
        }
    }
}