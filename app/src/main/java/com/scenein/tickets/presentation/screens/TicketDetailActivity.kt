package com.scenein.tickets.presentation.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.scenein.utils.SP
import com.scenein.R
import com.scenein.databinding.ActivityTicketDetailBinding
import com.scenein.tickets.data.models.Ticket
import com.scenein.tickets.presentation.view_model.TicketViewModel
import com.scenein.utils.DateTimeUtils
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.NetworkState
import org.json.JSONObject
import java.util.Locale

class TicketDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTicketDetailBinding
    private val viewModel: TicketViewModel by viewModels()
    private var ticket: Ticket? = null

    private val checkoutLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Purchase successful! Find your ticket in 'My Tickets'.", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_OK) // Set result for previous screen if it needs to refresh
            finish()
        } else {
            Toast.makeText(this, "Payment was cancelled or failed.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityTicketDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ticket = intent.getSerializableExtra("TICKET_DATA") as? Ticket
        if (ticket == null) {
            Toast.makeText(this, "Error: Could not load ticket details.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        populateUi()
        setupClickListener()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun populateUi() {
        ticket?.let {
            binding.tvEventName.text = it.eventName
            binding.tvEventVenue.text = it.eventVenue
            binding.tvSellerName.text = it.sellerName
            binding.tvEventCategory.text = it.category_name?.uppercase(Locale.ROOT) ?: "EVENT"

            Glide.with(this)
                .load(it.sellerProfilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(binding.ivSellerProfilePic)

            // ✅ Use DateTimeUtils here
            val formattedDate = DateTimeUtils.formatEventDate(it.eventDate)
            val formattedTime = DateTimeUtils.formatEventTimeRange(it.eventTime, null) // no end time

            binding.tvEventDateTime.text = "$formattedDate • $formattedTime"

            calculateAndDisplayPrice(it.sellingPrice)
        }
    }


    private fun calculateAndDisplayPrice(price: String?) {
        val sellingPrice = price?.toDoubleOrNull() ?: 0.0
        val fee = sellingPrice * 0.05 // 5% buyer protection fee
        val total = sellingPrice + fee

        binding.tvSellingPrice.text = String.format(Locale.ENGLISH, "₹%,.2f", sellingPrice)
        binding.tvFeePrice.text = String.format(Locale.ENGLISH, "₹%,.2f", fee)
        binding.tvTotalPrice.text = String.format(Locale.ENGLISH, "₹%,.2f", total)
        binding.btnBuyTicket.text = String.format("Pay ₹%,.2f", total)
    }

    private fun setupClickListener() {
        binding.btnBuyTicket.setOnClickListener {
            val buyerId = SP.getString(this, SP.USER_ID)
            if (buyerId.isNullOrEmpty()) {
                Toast.makeText(this, "Please log in to purchase a ticket.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ticket!!.sellerId == buyerId) {
                Toast.makeText(this, "You cannot buy your own ticket.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.createOrder(
                ticketId = ticket!!.id,
            )
        }
    }

    private fun observeViewModel() {
        viewModel.createOrderState.observe(this) { state ->
            binding.progressBar.isVisible = state is NetworkState.Loading
            binding.btnBuyTicket.visibility = if (state is NetworkState.Loading) View.INVISIBLE else View.VISIBLE

            if (state is NetworkState.Success) {
                val orderData = state.data
                val intent = Intent(this, CheckoutActivity::class.java).apply {
                    putExtra("order_id", orderData.orderId)
                    putExtra("amount", orderData.amountInPaise)
                    putExtra("key_id", orderData.keyId)
                }
                checkoutLauncher.launch(intent)
            } else if (state is NetworkState.Error) {
                val errorMessage = try {
                    JSONObject(state.message!!).getString("message")
                } catch (e: Exception) {
                    state.message
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
}