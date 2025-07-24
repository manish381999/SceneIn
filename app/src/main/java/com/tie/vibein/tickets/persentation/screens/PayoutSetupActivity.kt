package com.tie.vibein.tickets.persentation.screens

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.databinding.ActivityPayoutSetupBinding
import com.tie.vibein.tickets.presentation.viewmodel.TicketViewModel
import com.tie.vibein.utils.NetworkState
import org.json.JSONObject

class PayoutSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPayoutSetupBinding
    private val viewModel: TicketViewModel by viewModels()
    private var currentMethod = "upi" // Default to UPI tab

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayoutSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTabs()
        setupClickListener()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupTabs() {
        binding.tabLayoutPayout.addTab(binding.tabLayoutPayout.newTab().setText("UPI"))
        binding.tabLayoutPayout.addTab(binding.tabLayoutPayout.newTab().setText("Bank Account"))
        binding.tabLayoutPayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentMethod = if (tab?.position == 0) "upi" else "bank"
                binding.upiFormContainer.visibility = if (currentMethod == "upi") View.VISIBLE else View.GONE
                binding.bankFormContainer.visibility = if (currentMethod == "bank") View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupClickListener() {
        binding.btnVerifyAndSave.setOnClickListener {
            val userId = SP.getString(this, SP.USER_ID)
            if (userId.isNullOrEmpty()) {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentMethod == "upi") {
                val vpa = binding.etUpiId.text.toString().trim()
                if (!vpa.contains("@")) {
                    binding.etUpiId.error = "Please enter a valid UPI ID"
                    return@setOnClickListener
                }
                viewModel.verifyUpiPayout(userId, vpa)
            } else { // Bank
                val name = binding.etAccountHolderName.text.toString().trim()
                val ifsc = binding.etIfscCode.text.toString().trim()
                val account = binding.etAccountNumber.text.toString().trim()
                if (name.isBlank() || ifsc.isBlank() || account.isBlank()) {
                    Toast.makeText(this, "All bank details are required.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.verifyBankPayout(userId, name, ifsc, account)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.verifyPayoutState.observe(this) { state ->
            binding.progressBar.isVisible = state is NetworkState.Loading
            binding.btnVerifyAndSave.isEnabled = state !is NetworkState.Loading
            binding.btnVerifyAndSave.text = if (state is NetworkState.Loading) "" else "Verify & Save"

            when (state) {
                is NetworkState.Success -> {
                    Toast.makeText(this, "Success: ${state.data.message}", Toast.LENGTH_LONG).show()

                    // --- THIS IS THE DEFINITIVE, CORRECT LOGIC ---
                    // 1. Save to SharedPreferences that the user is now verified.
                    SP.saveBoolean(this, SP.IS_PAYOUT_VERIFIED, true)

                    // 2. Create and save a display-friendly, masked version of the payout info.
                    val maskedInfo = if (currentMethod == "upi") {
                        "UPI: ${binding.etUpiId.text}"
                    } else {
                        val accNum = binding.etAccountNumber.text.toString()
                        "Bank: ****${accNum.takeLast(4)}"
                    }
                    SP.saveString(this, SP.PAYOUT_INFO_DISPLAY, maskedInfo)
                    // --- END OF CRITICAL LOGIC ---

                    // Set the result to OK so SellTicketActivity knows to refresh its UI.
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is NetworkState.Error -> {
                    val errorMessage = try { JSONObject(state.message!!).getString("message") } catch(e: Exception) { state.message }
                    Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }
}