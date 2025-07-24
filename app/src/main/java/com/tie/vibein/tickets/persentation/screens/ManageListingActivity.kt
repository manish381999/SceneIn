package com.tie.vibein.tickets.persentation.screens

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.databinding.ActivityManageListingBinding
import com.tie.vibein.tickets.data.models.Ticket
import com.tie.vibein.tickets.presentation.viewmodel.TicketViewModel
import com.tie.vibein.utils.NetworkState
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ManageListingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageListingBinding
    private val viewModel: TicketViewModel by viewModels()
    private var ticket: Ticket? = null
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageListingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ticket = intent.getSerializableExtra("TICKET_DATA") as? Ticket
        currentUserId = SP.getString(this, SP.USER_ID, "")!!

        // Final safety check. If ticket is null or data is missing, finish immediately.
        if (ticket == null || currentUserId.isEmpty() || ticket?.originalPrice == null) {
            Toast.makeText(this, "Error: Could not load listing details. Please try again.", Toast.LENGTH_LONG).show()
            finish()
            return
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
            // Populate all the read-only and editable fields
            binding.tvEventName.text = it.eventName
            binding.tvEventVenue.text = it.eventVenue
            binding.etSellingPrice.setText(it.sellingPrice)
            binding.etNumberOfTickets.text = "${it.numberOfTickets} Ticket(s)"

            // Format date and time for the read-only display
            formatDateTime(it.eventDate, it.eventTime)

            // Populate the info text using the originalPrice from the ticket object
            val originalPrice = it.originalPrice.toDoubleOrNull() ?: 0.0
            binding.tvOriginalPriceInfo.text = "Original price was ₹${originalPrice.toInt()}. You cannot list for higher."
        }
    }

    private fun formatDateTime(dateStr: String?, timeStr: String?) {
        if (dateStr.isNullOrBlank() || timeStr.isNullOrBlank()) {
            binding.tvEventDateTime.text = "Date/Time not specified"
            return
        }
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(dateStr)
            val time = SimpleDateFormat("HH:mm:ss", Locale.ROOT).parse(timeStr)
            val formattedDate = date?.let { d -> SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(d) } ?: ""
            val formattedTime = time?.let { t -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(t) } ?: ""
            binding.tvEventDateTime.text = "$formattedDate • $formattedTime"
        } catch (e: Exception) {
            binding.tvEventDateTime.text = "$dateStr • $timeStr"
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveChanges.setOnClickListener {
            handleUpdatePrice()
        }
        binding.btnDelistTicket.setOnClickListener {
            showDelistConfirmationDialog()
        }
    }

    private fun handleUpdatePrice() {
        val newPriceString = binding.etSellingPrice.text.toString().trim()
        val newPrice = newPriceString.toDoubleOrNull()
        val originalPrice = ticket?.originalPrice?.toDoubleOrNull() ?: 0.0

        // Perform rigorous front-end validation for a great UX.
        if (newPrice == null || newPrice <= 0) {
            Toast.makeText(this, "Please enter a valid selling price.", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPrice > originalPrice) {
            Toast.makeText(this, "Selling price cannot be higher than the original price of ₹${originalPrice.toInt()}.", Toast.LENGTH_SHORT).show()
            return
        }

        // If validation passes, call the ViewModel. The backend will validate again for security.
        viewModel.updateTicket(ticket!!.id, currentUserId, newPriceString)
    }

    private fun showDelistConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delist Ticket?")
            .setMessage("Are you sure you want to remove this ticket from the marketplace? It will no longer be visible to buyers but will remain in your history.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Yes, Delist") { _, _ ->
                viewModel.delistTicket(ticket!!.id, currentUserId)
            }
            .show()
    }

    private fun observeViewModel() {
        viewModel.listingActionState.observe(this) { state ->
            setLoading(state is NetworkState.Loading)
            when (state) {
                is NetworkState.Success -> {
                    Toast.makeText(this, state.data.message, Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK) // Signal to MyTicketsFragment to refresh
                    finish()
                }
                is NetworkState.Error -> {
                    val errorMessage = try {
                        // Try to parse the specific JSON error message from the PHP script
                        JSONObject(state.message!!).getString("message")
                    } catch (e: Exception) {
                        // Fallback to the generic error message if parsing fails
                        state.message
                    }
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Update Failed")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show()
                }
                else -> {
                    // This handles the case where the state is `null` or `NetworkState.Loading`
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        val areButtonsEnabled = !isLoading
        binding.btnSaveChanges.isEnabled = areButtonsEnabled
        binding.btnDelistTicket.isEnabled = areButtonsEnabled
        binding.etSellingPrice.isEnabled = areButtonsEnabled // Also disable input during loading
        binding.btnSaveChanges.text = if(isLoading) "" else "Save Price Changes"
    }
}