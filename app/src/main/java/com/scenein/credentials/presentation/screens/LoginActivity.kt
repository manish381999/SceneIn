package com.scenein.credentials.presentation.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.scenein.R
// AuthRepository and AuthViewModelFactory imports are no longer needed here
import com.scenein.credentials.presentation.view_model.AuthViewModel
import com.scenein.databinding.ActivityLoginBinding
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.NetworkState

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // --- UPDATED: ViewModel initialization no longer needs the factory ---
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onClickListener()
        observeViewModel()
    }

    private fun onClickListener() {
        setupPhoneInputWatcher()

        binding.btnContinue.setOnClickListener {
            // Check if the button is enabled before proceeding
            if (!binding.btnContinue.isEnabled) {
                Toast.makeText(this, "Please enter a valid 10-digit mobile number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val mobile = binding.etMobileNumber.text.toString().trim()
            val countryCode = binding.ccp.selectedCountryCodeWithPlus
            val countryShortName = binding.ccp.selectedCountryNameCode

            viewModel.loginWithOtp(mobile, countryCode, countryShortName)
        }

        binding.tvTermsOfService.setOnClickListener {
            openUrl("https://scenein.in/terms")
        }

        binding.tvPrivacyPolicy.setOnClickListener {
            openUrl("https://scenein.in/privacy")
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        // This check ensures you have an app that can handle the URL (like a browser)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPhoneInputWatcher() {
        binding.etMobileNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString()
                // The button is enabled only for a 10-digit number
                binding.btnContinue.isEnabled = input.length == 10
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    // You can show a progress bar here and disable the button
                    binding.btnContinue.isEnabled = false
                    Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show()
                }
                is NetworkState.Success -> {
                    binding.btnContinue.isEnabled = true // Re-enable on success
                    Log.d("API_RESPONSE", "Response: ${state.data}")

                    val mobile = binding.etMobileNumber.text.toString().trim()
                    val countryCode = binding.ccp.selectedCountryCodeWithPlus
                    val countryShortName = binding.ccp.selectedCountryNameCode
                    val otp = state.data.otp

                    Log.d("OTP_DEBUG", "OTP received: $otp")

                    if (otp != null) {
                        showOtpNotification(otp)
                        val intent = Intent(this, OtpVerificationActivity::class.java).apply {
                            putExtra("mobile_number", mobile)
                            putExtra("country_code", countryCode)
                            putExtra("country_short_name", countryShortName)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Error: OTP not received in response", Toast.LENGTH_SHORT).show()
                    }
                }
                is NetworkState.Error -> {
                    binding.btnContinue.isEnabled = true // Re-enable on error
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showOtpNotification(otp: String) {
        val channelId = "otp_channel"
        val channelName = "OTP Notifications"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "This channel is used for OTP notifications."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle("VibeIn Verification Code")
            .setContentText("Your code is $otp. Do not share it with anyone.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }
}