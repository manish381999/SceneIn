package com.scenein.settings.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scenein.R
import com.scenein.databinding.ItemTransactionHistoryBinding
import com.scenein.settings.data.models.TransactionHistoryItem
import com.scenein.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Locale

class TicketTransactionHistoryAdapter(
    private val onTransactionClick: (TransactionHistoryItem) -> Unit
) : ListAdapter<TransactionHistoryItem, TicketTransactionHistoryAdapter.TransactionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position), onTransactionClick)
    }

    class TransactionViewHolder(private val binding: ItemTransactionHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionHistoryItem, onTransactionClick: (TransactionHistoryItem) -> Unit) {
            val context = binding.root.context

            // Populate common text fields
            binding.tvEventName.text = "For: ${item.eventName}"
            binding.tvTransactionDate.text = DateTimeUtils.formatEventDate(item.createdAt, "dd MMM yyyy 'at' hh:mm a")


            // Determine UI style based on user's role (BOUGHT vs. SOLD)
            if (item.userRole == "BOUGHT") {
                // --- User BOUGHT a ticket (Money Out) ---
                binding.tvTransactionSubtitle.text = "Paid to: ${item.otherUserName}"
                binding.tvTransactionAmount.text = String.format(Locale.ENGLISH, "- ₹%,.0f", item.finalAmount.toDoubleOrNull() ?: 0.0)

                // Use a helper function to set the title and icon based on status
                setBuyerView(item, context)
            } else { // SOLD
                // --- User SOLD a ticket (Money In) ---
                binding.tvTransactionSubtitle.text = "Payment from: ${item.otherUserName}"
                binding.tvTransactionAmount.text = String.format(Locale.ENGLISH, "+ ₹%,.0f", item.sellerPayoutAmount.toDoubleOrNull() ?: 0.0)

                // Use a helper function to set the title and icon based on status
                setSellerView(item, context)
            }

            itemView.setOnClickListener { onTransactionClick(item) }
        }

        private fun setBuyerView(item: TransactionHistoryItem, context: Context) {
            when (item.transactionStatus.lowercase()) {
                "escrow", "completed_by_user", "completed_auto", "resold", "payout_queued", "payout_processed" -> {
                    binding.tvTransactionTitle.text = "Ticket Purchased"
                    setIcon(R.drawable.ic_check_circle, R.color.colorSuccess, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
                }
                "in_dispute" -> {
                    binding.tvTransactionTitle.text = "Purchase On Hold"
                    binding.tvTransactionSubtitle.text= "This transaction is currently under review."
                    setIcon(R.drawable.ic_error, R.color.colorWarning, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
                }


                "refund_queued", "refunded" -> {
                    binding.tvTransactionTitle.text = "Refund in Progress"
                    binding.tvTransactionSubtitle.text= "Your refund is being processed and should arrive soon."
                    setIcon(R.drawable.ic_history, R.color.colorWarning, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.colorSuccess)) // Money back is good
                }
                "refund_processed" -> {
                    binding.tvTransactionTitle.text = "Payment Refunded"
                    binding.tvTransactionSubtitle.text= "The amount has been returned to your original payment method."
                    setIcon(R.drawable.ic_check_circle, R.color.colorPrimary, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.colorSuccess)) // Money back is good
                }
                "failed" -> {
                    binding.tvTransactionTitle.text = "Payment Failed"
                    setIcon(R.drawable.ic_error, R.color.colorError, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.colorError))
                }
                else -> {
                    binding.tvTransactionTitle.text = "Purchase Processing"
                    setIcon(R.drawable.ic_help, R.color.textSecondary, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
                }
            }
        }

        private fun setSellerView(item: TransactionHistoryItem, context: Context) {
            when (item.transactionStatus.lowercase()) {
                "escrow" -> {
                    binding.tvTransactionTitle.text = "Payment Held in Escrow"
                    setIcon(R.drawable.ic_history, R.color.colorWarning, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.colorSuccess))
                }

                "payout_queued",  "completed_by_user", "completed_auto", "resold" -> {
                    binding.tvTransactionTitle.text = "Payout Processing"
                    setIcon(R.drawable.ic_history, R.color.colorPrimary, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.colorSuccess))
                }

                "payout_processed" -> {
                    binding.tvTransactionTitle.text = "Payout Complete"
                    setIcon(R.drawable.ic_check_circle, R.color.colorSuccess, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.colorSuccess))
                }
                "in_dispute" -> {
                    binding.tvTransactionTitle.text = "Payout On Hold"
                    binding.tvTransactionSubtitle.text= "Awaiting dispute resolution."
                    setIcon(R.drawable.ic_error, R.color.colorWarning, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.colorWarning))
                }
                "refunded", "refund_queued", "refund_processed" -> {
                    binding.tvTransactionTitle.text = "Transaction Refunded to Buyer"
                    binding.tvTransactionSubtitle.text= "This payment was returned to the buyer."
                    setIcon(R.drawable.ic_warning, R.color.colorError, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.textSecondary))
                }
                else -> {
                    binding.tvTransactionTitle.text = "Sale Processing"
                    setIcon(R.drawable.ic_help, R.color.textSecondary, context)
                    binding.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
                }
            }
        }

        /**
         * Helper function to set the icon, icon color, and background color.
         */
        private fun setIcon(iconRes: Int, colorRes: Int, context: Context) {
            binding.ivTransactionType.setImageResource(iconRes)
            binding.ivTransactionType.setColorFilter(ContextCompat.getColor(context, colorRes))
            // This is a simple way to create matching backgrounds
            val backgroundDrawable = when(colorRes) {
                R.color.colorSuccess -> R.drawable.bg_icon_success
                R.color.colorError -> R.drawable.bg_icon_error
                R.color.colorWarning -> R.drawable.bg_icon_warning // You would create this
                else -> R.drawable.bg_icon_neutral
            }
            binding.iconContainer.setBackgroundResource(backgroundDrawable)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TransactionHistoryItem>() {
            override fun areItemsTheSame(old: TransactionHistoryItem, new: TransactionHistoryItem): Boolean = old.transactionId == new.transactionId
            override fun areContentsTheSame(old: TransactionHistoryItem, new: TransactionHistoryItem): Boolean = old == new
        }
    }
}