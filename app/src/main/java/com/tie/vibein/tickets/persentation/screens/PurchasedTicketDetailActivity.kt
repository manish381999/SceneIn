package com.tie.vibein.tickets.persentation.screens

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.R
import com.tie.vibein.databinding.ActivityPurchasedTicketDetailBinding
import com.tie.vibein.tickets.data.models.Ticket
import com.tie.vibein.tickets.presentation.viewmodel.TicketViewModel
import com.tie.vibein.utils.NetworkState
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class PurchasedTicketDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPurchasedTicketDetailBinding
    private val viewModel: TicketViewModel by viewModels()
    private var ticket: Ticket? = null
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchasedTicketDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getString(this, SP.USER_ID) ?: ""
        ticket = intent.getSerializableExtra("TICKET_DATA") as? Ticket

        if (ticket == null || currentUserId.isEmpty() || ticket?.transactionId == null) {
            Toast.makeText(this, "Error: Could not load purchased ticket details.", Toast.LENGTH_LONG).show()
            finish(); return
        }

        setupToolbar()
        populateUi()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun populateUi() {
        ticket?.let {
            binding.tvEventName.text = it.eventName
            binding.tvEventVenue.text = it.eventVenue
            formatDateTime(it.eventDate, it.eventTime)
            loadRealTicketImage(it.secureFilePath)
            updateUiForTransactionStatus()
        }
    }

    private fun formatDateTime(dateStr: String?, timeStr: String?) {
        if (dateStr.isNullOrBlank() || timeStr.isNullOrBlank()) {
            binding.tvEventDateTime.text = "Date & Time not specified"
            return
        }
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(dateStr)
            val time = SimpleDateFormat("HH:mm:ss", Locale.ROOT).parse(timeStr)
            val formattedDate = date?.let { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(it) } ?: ""
            val formattedTime = time?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it) } ?: ""
            binding.tvEventDateTime.text = "$formattedDate  •  $formattedTime"
        } catch (e: Exception) {
            Log.e("DateTimeFormatError", "Failed to format date/time", e)
            binding.tvEventDateTime.text = "$dateStr at $timeStr"
        }
    }

    private fun loadRealTicketImage(secureFilePath: String?) {
        if (secureFilePath.isNullOrBlank()) {
            binding.ivRealTicket.setImageResource(R.drawable.ic_ticket_placeholder)
            return
        }
        val fileName = secureFilePath.substringAfterLast("/")
        val publicUrl = "https://dreamsquad.fun/uploads/tickets/$fileName"
        Glide.with(this)
            .load(publicUrl)
            .placeholder(R.drawable.loading_placeholder)
            .error(R.drawable.ic_ticket_placeholder)
            .into(binding.ivRealTicket)
    }

    private fun updateUiForTransactionStatus() {
        val status = ticket?.transactionStatus?.lowercase(Locale.ROOT)
        val completionType = ticket?.completionType?.lowercase(Locale.ROOT)
        val hasBeenRevealed = !ticket?.revealTime.isNullOrBlank()

        // Default UI state: All action buttons hidden, message shown, ticket blurred.
        binding.initialActionsContainer.isVisible = false
        binding.confirmationContainer.isVisible = false
        binding.tvUsedMessage.isVisible = true
        binding.ticketImageCard.isVisible = true
        binding.revealOverlay.isVisible = true

        // This is the definitive state machine for the buyer's ticket detail UI.
        when {
            completionType == "resold" -> {
                binding.tvUsedMessage.text = "You have successfully resold this ticket."
                binding.ticketImageCard.isVisible = false // Hide the ticket image entirely for security.
            }
            status == "refunded" || status == "refund_processed" -> {
                binding.tvUsedMessage.text = "This purchase has been refunded."
                binding.ticketImageCard.isVisible = false
            }
            status == "refund_queued" || status == "refund_actioned" -> {
                binding.tvUsedMessage.text = "Your refund for this ticket is being processed."
            }
            status == "escrow" && hasBeenRevealed -> {
                binding.revealOverlay.isVisible = false
                binding.confirmationContainer.isVisible = true
                binding.tvUsedMessage.isVisible = false
            }
            status == "escrow" && !hasBeenRevealed -> {
                binding.initialActionsContainer.isVisible = true
                binding.tvUsedMessage.isVisible = false
            }
            status == "in_dispute" -> {
                binding.tvUsedMessage.text = "This ticket issue is under review."
            }
            status in listOf("completed_by_user", "completed_auto", "payout_queued", "payout_processed") -> {
                binding.tvUsedMessage.text = "This ticket has been used. Hope you enjoyed the event!"
                binding.revealOverlay.isVisible = false
            }
            else -> {
                binding.tvUsedMessage.text = "This ticket is in an unknown state."
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnReveal.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Reveal Secure Ticket?")
                .setMessage("Only do this when you are at the event gate and ready for scanning. This action is logged for security.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reveal Now") { _, _ ->
                    ticket?.transactionId?.toIntOrNull()?.let {
                        viewModel.revealTicket(it, currentUserId)
                    }
                }.show()
        }
        binding.btnConfirmEntry.setOnClickListener {
            ticket?.transactionId?.toIntOrNull()?.let {
                viewModel.completeTransaction(it, currentUserId)
            }
        }
        binding.btnReportIssue.setOnClickListener {
            showDisputeDialog()
        }
        binding.btnResell.setOnClickListener {
            showResellDialog()
        }
    }

    private fun showDisputeDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null, false)
        val editText = dialogView.findViewById<EditText>(R.id.editText)
        editText.hint = "e.g., QR code was invalid, entry denied..."

        MaterialAlertDialogBuilder(this)
            .setTitle("Report an Issue")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Submit Report") { _, _ ->
                val reason = editText.text.toString().trim()
                if (reason.length > 10) {
                    ticket?.transactionId?.toIntOrNull()?.let { viewModel.createDispute(it, currentUserId, reason) }
                } else {
                    Toast.makeText(this, "Please provide a detailed reason.", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun showResellDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null, false)
        val editText = dialogView.findViewById<EditText>(R.id.editText)
        val originalPrice = ticket?.sellingPrice?.toDoubleOrNull() ?: 0.0

        editText.hint = "Your Selling Price (Max ₹${originalPrice.toInt()})"
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        MaterialAlertDialogBuilder(this)
            .setTitle("Resell Your Ticket")
            .setMessage("Your current ticket will be invalidated, and a new listing will be created for sale.")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("List for Resale") { _, _ ->
                val newSellingPrice = editText.text.toString().toDoubleOrNull()
                if (newSellingPrice == null || newSellingPrice <= 0) { Toast.makeText(this, "Please enter a valid price.", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                if (newSellingPrice > originalPrice) { Toast.makeText(this, "Resale price cannot be higher than what you paid.", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

                ticket?.transactionId?.toIntOrNull()?.let { viewModel.relistTicket(it, currentUserId, newSellingPrice.toString()) }
            }.show()
    }

    private fun observeViewModel() {
        viewModel.transactionActionState.observe(this) { state ->
            setLoading(state is NetworkState.Loading)
            when (state) {
                is NetworkState.Success -> {
                    Toast.makeText(this, state.data.message, Toast.LENGTH_LONG).show()
                    val message = state.data.message
                    if (message.contains("reveal time recorded")) {
                        ticket = ticket?.copy(revealTime = "revealed")
                        updateUiForTransactionStatus()
                    } else {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
                is NetworkState.Error -> {
                    val errorMessage = try { JSONObject(state.message!!).getString("message") } catch (e: Exception) { state.message }
                    Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        val buttonsVisibility = if(isLoading) View.INVISIBLE else View.VISIBLE
        if (binding.initialActionsContainer.isVisible || isLoading) {
            binding.initialActionsContainer.visibility = buttonsVisibility
        }
        if (binding.confirmationContainer.isVisible || isLoading) {
            binding.confirmationContainer.visibility = buttonsVisibility
        }
    }
}