package com.scenein.settings.persentation.screen

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.databinding.ActivityTicketTransactionDetailBinding
import com.scenein.settings.data.models.TransactionDetail
import com.scenein.settings.persentation.view_model.SettingsViewModel
import com.scenein.utils.NetworkState
import java.text.SimpleDateFormat
import java.util.Locale

class TicketTransactionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTicketTransactionDetailBinding
    private val viewModel: SettingsViewModel by viewModels()
    private var transactionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTicketTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionId = intent.getIntExtra("TRANSACTION_ID", 0)
        if (transactionId == 0) {
            Toast.makeText(this, "Error: Transaction ID is missing.", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        setupToolbar()
        observeViewModel()
        viewModel.fetchTransactionDetails(transactionId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.transactionDetailState.observe(this) { state ->
            binding.progressBar.isVisible = state is NetworkState.Loading
            binding.contentScrollView.isVisible = state is NetworkState.Success

            if (state is NetworkState.Success) {
                populateUi(state.data)
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- REVISED AND CORRECTED LOGIC ---
    private fun populateUi(details: TransactionDetail) {
        // --- Populate common fields ---
        binding.tvTransactionId.text =
            details.receiptId // Assuming receiptId is your payment_gateway_order_id
        binding.tvEventNameDetail.text = details.eventName
        formatDateTime(details.createdAt)

        binding.tvOtherUserName.text = details.otherUserName
        Glide.with(this)
            .load(details.otherUserProfilePic)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(binding.ivOtherUserPic)

        val finalAmount = details.finalAmount.toDoubleOrNull() ?: 0.0
        val platformFee = details.platformFee.toDoubleOrNull() ?: 0.0
        val payoutAmount = details.sellerPayoutAmount.toDoubleOrNull() ?: 0.0
        val ticketAmount = finalAmount - platformFee

        binding.tvTicketAmount.text = String.format(Locale.ENGLISH, "₹%,.2f", ticketAmount)
        binding.tvFeeAmount.text = String.format(Locale.ENGLISH, "₹%,.2f", platformFee)


        // --- Core logic to handle different statuses ---
        if (details.userRole == "BOUGHT") {
            populateUiForBuyer(details, finalAmount)
        } else { // SOLD
            populateUiForSeller(details, payoutAmount)
        }
    }

    private fun populateUiForBuyer(details: TransactionDetail, totalPaid: Double) {
        binding.tvOtherUserLabel.text = "PAID TO"
        binding.tvTotalLabel.text = "You Paid"
        binding.tvTotalAmount.text = String.format(Locale.ENGLISH, "₹%,.2f", totalPaid)
        binding.tvStatusAmount.text = String.format(Locale.ENGLISH, "- ₹%,.2f", totalPaid)

        when (details.transactionStatus) {
            "escrow", "payout_queued", "completed_by_user", "completed_auto", "payout_processed", "resold" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorSuccess
                    )
                )
                binding.tvStatusTitle.text = "Payment Successful"
                binding.tvStatusSubtitle.text = "Your payment is secured by SceneIn Escrow."
            }

            "in_dispute" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_error)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorWarning
                    )
                )
                binding.tvStatusTitle.text = "Payment On Hold"
                binding.tvStatusSubtitle.text = "This transaction is currently under review."
            }

            "refunded", "refund_queued" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_history)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorWarning
                    )
                )
                binding.tvStatusTitle.text = "Refund in Progress"
                binding.tvStatusSubtitle.text = "Your refund is being processed and should arrive soon."
            }

            "refund_processed" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorPrimary
                    )
                )
                binding.tvStatusTitle.text = "Payment Refunded"
                binding.tvStatusSubtitle.text = "The amount has been returned to your original payment method."
            }


            "failed" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_warning)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorError
                    )
                )
                binding.tvStatusTitle.text = "Payment Failed"
                binding.tvStatusSubtitle.text = "Your payment could not be processed."
            }

            "pending_payment" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_history)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorWarning
                    )
                )
                binding.tvStatusTitle.text = "Payment Pending"
                binding.tvStatusSubtitle.text = "Please complete your payment to secure the ticket."
            }

            else -> {
                binding.tvStatusTitle.text = details.transactionStatus.replaceFirstChar { it.titlecase() }
                binding.tvStatusSubtitle.text = "Status: ${details.transactionStatus}"
            }
        }
    }

    private fun populateUiForSeller(details: TransactionDetail, payoutAmount: Double) {
        binding.tvOtherUserLabel.text = "RECEIVED FROM"
        binding.tvTotalLabel.text = "Your Payout"
        binding.tvTotalAmount.text = String.format(Locale.ENGLISH, "₹%,.2f", payoutAmount)
        binding.tvStatusAmount.text = String.format(Locale.ENGLISH, "+ ₹%,.2f", payoutAmount)

        when (details.transactionStatus) {
            "escrow" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_history)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorWarning
                    )
                )
                binding.tvStatusTitle.text = "Payment Held in Escrow"
                binding.tvStatusSubtitle.text = "Payout scheduled after event completion."
            }

            "in_dispute" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_error)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorWarning
                    )
                )
                binding.tvStatusTitle.text = "Payout On Hold"
                binding.tvStatusSubtitle.text = "Awaiting dispute resolution."
            }

           "resold", "payout_queued", "completed_by_user", "completed_auto" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_history)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorPrimary
                    )
                )
                binding.tvStatusTitle.text = "Payout Processing"
                binding.tvStatusSubtitle.text = "Your payout is being processed."
            }

            "payout_processed" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorSuccess
                    )
                )
                binding.tvStatusTitle.text = "Payout Complete"
                binding.tvStatusSubtitle.text = "The amount has been sent to your account."
            }

            "refunded", "refund_queued", "refund_processed" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_warning)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorError
                    )
                )
                binding.tvStatusTitle.text = "Transaction Refunded"
                binding.tvStatusSubtitle.text = "This payment was returned to the buyer."
            }

            "failed" -> {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_warning)
                binding.ivStatusIcon.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        R.color.colorError
                    )
                )
                binding.tvStatusTitle.text = "Transaction Failed"
                binding.tvStatusSubtitle.text = "The buyer's payment failed."
            }

            else -> {
                binding.tvStatusTitle.text = details.transactionStatus.replaceFirstChar { it.titlecase() }
                binding.tvStatusSubtitle.text = "Status: ${details.transactionStatus}"
            }
        }
    }

    private fun formatDateTime(createdAt: String?) {
        if (createdAt.isNullOrBlank()) {
            binding.tvDateTime.text = "Date not available"
            return
        }
        try {
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).parse(createdAt)
            binding.tvDateTime.text = date?.let {
                SimpleDateFormat(
                    "dd MMM yyyy 'at' hh:mm a",
                    Locale.getDefault()
                ).format(it)
            } ?: createdAt
        } catch (e: Exception) {
            Log.e("DateTimeFormatError", "Failed to format transaction detail date", e)
            binding.tvDateTime.text = createdAt
        }
    }
}