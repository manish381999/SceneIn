package com.tie.vibein.credentials.presentation.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.tie.vibein.R
import com.tie.vibein.credentials.data.repository.AuthRepository
import com.tie.vibein.credentials.presentation.view_model.AuthViewModel
import com.tie.vibein.credentials.presentation.view_model.AuthViewModelFactory
import com.tie.vibein.databinding.ActivityLoginBinding
import com.tie.vibein.utils.NetworkState

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initComponents()
        onClickListener()
        observeViewModel()
    }

    private fun initComponents() {
        // Any other component initialization
    }

    private fun onClickListener() {
        setupPhoneInputWatcher()

        binding.btnContinue.setOnClickListener {
            val mobile = binding.etMobileNumber.text.toString().trim()
            val countryCode = binding.ccp.selectedCountryCodeWithPlus
            val countryShortName = binding.ccp.selectedCountryNameCode

            if (mobile.length == 10) {
                viewModel.loginWithOtp(mobile, countryCode, countryShortName)

                // ✅ Get FCM Token and send it
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d("FCM", "Token: $token")
                        viewModel.sendFcmToken(mobile, token)
                    } else {
                        Log.e("FCM", "Fetching FCM token failed", task.exception)
                    }
                }
            }
        }
    }

    private fun setupPhoneInputWatcher() {
        binding.etMobileNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString().trim()

                if (input.length == 10 && input.all { it.isDigit() }) {
                    binding.btnContinue.setBackgroundResource(R.drawable.bg_login_btn_enable)
                    binding.btnContinue.setTextColor(ContextCompat.getColor(this@LoginActivity, android.R.color.black))
                    binding.btnContinue.isEnabled = true
                } else {
                    binding.btnContinue.setBackgroundResource(R.drawable.bg_login_btn_disable)
                    binding.btnContinue.setTextColor(ContextCompat.getColor(this@LoginActivity, android.R.color.white))
                    binding.btnContinue.isEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show()
                }

                is NetworkState.Success -> {
                    Log.d("API_RESPONSE", "Response: ${state.data}")

                    val mobile = binding.etMobileNumber.text.toString().trim()
                    val countryCode = binding.ccp.selectedCountryCodeWithPlus
                    val countryShortName = binding.ccp.selectedCountryNameCode
                    val otp = state.data.otp

                    Log.d("OTP_DEBUG", "OTP received: $otp")

                    if (otp != null) {
                        showOtpNotification(otp)
                        val intent = Intent(this, OtpVerificationActivity::class.java)
                        intent.putExtra("mobile_number", mobile)
                        intent.putExtra("country_code", countryCode)
                        intent.putExtra("country_short_name", countryShortName)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Error: OTP not received", Toast.LENGTH_SHORT).show()
                    }
                }

                is NetworkState.Error -> {
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.fcmTokenState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    Log.d("FCM", "Sending FCM token...")
                }

                is NetworkState.Success -> {
                    Log.d("FCM", "Token sent: ${state.data}")
                }

                is NetworkState.Error -> {
                    Log.e("FCM", "Failed to send FCM token: ${state.message ?: "No error message"}")
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
            .setContentTitle("Your OTP Code")
            .setContentText("Your OTP is: $otp")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }
}
