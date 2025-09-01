package com.scenein.credentials.presentation.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.firebase.messaging.FirebaseMessaging
import com.scenein.utils.SP
import com.scenein.BaseActivity
import com.scenein.R
import com.scenein.credentials.data.models.UserData
import com.scenein.credentials.data.repository.AuthRepository
import com.scenein.credentials.presentation.view_model.AuthViewModel
import com.scenein.credentials.presentation.view_model.AuthViewModelFactory
import com.scenein.databinding.ActivityOtpVerificationBinding
import com.scenein.utils.DeviceUtils
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.NetworkState
import java.util.regex.Pattern

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpVerificationBinding
    private lateinit var mobileNumber: String
    private lateinit var countryCode: String
    private lateinit var countryShortName: String

    private var countDownTimer: CountDownTimer? = null
    private lateinit var viewModel: AuthViewModel

    private val smsConsentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val message = result.data!!.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            message?.let { extractOtpFromMessage(it) }
        } else {
            Log.d("OTP_AUTOFILL", "Consent was denied or flow was cancelled.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentData()
        setupViewModel()
        initComponents()
        setupOtpInputs()
        onClickListener()
        observeViewModel()
        startSmsListener()
    }

    private fun startSmsListener() {
        SmsRetriever.getClient(this).startSmsUserConsent(null)
            .addOnSuccessListener { Log.d("OTP_AUTOFILL", "SMS User Consent listener started.") }
            .addOnFailureListener { e -> Log.e("OTP_AUTOFILL", "Error starting SMS User Consent listener.", e) }
    }

    private fun extractOtpFromMessage(message: String) {
        val pattern = Pattern.compile("(\\d{6})")
        val matcher = pattern.matcher(message)
        if (matcher.find()) {
            val otp = matcher.group(0)
            otp?.let {
                Log.d("OTP_AUTOFILL", "OTP extracted: $it")
                fillOtpFields(it)
                verifyOtp(mobileNumber, it)
            }
        }
    }

    private fun fillOtpFields(otp: String) {
        val editTexts = listOf(binding.etOtp1, binding.etOtp2, binding.etOtp3, binding.etOtp4, binding.etOtp5, binding.etOtp6)
        otp.forEachIndexed { index, char ->
            if (index < editTexts.size) {
                editTexts[index].setText(char.toString())
            }
        }
        hideKeyboard()
    }

    private fun setupViewModel() {
        val repository = AuthRepository()
        val viewModelFactory = AuthViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[AuthViewModel::class.java]
    }

    private fun getIntentData() {
        mobileNumber = intent.getStringExtra("mobile_number") ?: ""
        countryCode = intent.getStringExtra("country_code") ?: ""
        countryShortName = intent.getStringExtra("country_short_name") ?: ""
    }

    private fun initComponents() {
        binding.tvSubtitle.text = "We have sent a verification code to $countryCode $mobileNumber"
        binding.etOtp1.requestFocus()
        showKeyboard(binding.etOtp1)
        startResendTimer()
    }

    private fun setupOtpInputs() {
        val editTexts = listOf(binding.etOtp1, binding.etOtp2, binding.etOtp3, binding.etOtp4, binding.etOtp5, binding.etOtp6)
        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener {
                if (it?.length == 1 && i < editTexts.size - 1) {
                    editTexts[i + 1].requestFocus()
                }
                checkOtpCompletion()
            }
            if (i > 0) {
                editTexts[i].setOnKeyListener { _, keyCode, _ ->
                    if (keyCode == KeyEvent.KEYCODE_DEL && editTexts[i].text.isEmpty()) {
                        editTexts[i - 1].requestFocus()
                        true
                    } else false
                }
            }
        }
    }

    private fun checkOtpCompletion() {
        val editTexts = listOf(binding.etOtp1, binding.etOtp2, binding.etOtp3, binding.etOtp4, binding.etOtp5, binding.etOtp6)
        val otpString = editTexts.joinToString("") { it.text.toString() }
        binding.btnContinue.isEnabled = otpString.length == 6
    }

    private fun startResendTimer() {
        binding.btnSms.isEnabled = false
        binding.btnWhatsapp.isEnabled = false
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(30_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                binding.tvResendTimer.text = "Didn’t get the OTP? (Request again in 0:${String.format("%02d", secondsLeft)}s)"
            }
            override fun onFinish() {
                binding.tvResendTimer.text = "Didn’t get the OTP? Resend via:"
                binding.btnSms.isEnabled = true
                binding.btnWhatsapp.isEnabled = true
            }
        }.start()
    }

    private fun onClickListener() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.tvBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        binding.btnSms.setOnClickListener {
            if (binding.btnSms.isEnabled) {
                viewModel.loginWithOtp(mobileNumber, countryCode, countryShortName)
                Toast.makeText(this, "OTP resent via SMS", Toast.LENGTH_SHORT).show()
                startResendTimer()
            }
        }
        binding.btnContinue.setOnClickListener {
            if (binding.btnContinue.isEnabled) {
                val otpString = listOf(binding.etOtp1, binding.etOtp2, binding.etOtp3, binding.etOtp4, binding.etOtp5, binding.etOtp6)
                    .joinToString("") { it.text.toString() }
                verifyOtp(mobileNumber, otpString)
                hideKeyboard()
            }
        }
    }

    private fun observeViewModel() {
        // Observer for the OTP verification result
        viewModel.verifyState.observe(this) { networkState ->
            // --- This observer now has updated logic to handle the auth_token ---
            when (networkState) {
                is NetworkState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnContinue.isEnabled = false
                }
                is NetworkState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val response = networkState.data
                    val user = response?.user
                    val authToken = response?.authToken

                    if (user != null && !authToken.isNullOrEmpty()) {
                        // SUCCESS! We have a user and an auth token.
                        SP.saveString(this, SP.AUTH_TOKEN, authToken)
                        saveUserData(user) // Save the rest of the user data
                        Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()

                        val nextIntent = if (user.name.isNullOrEmpty()) {
                            Intent(this, OnboardingActivity::class.java)
                        } else {
                            SP.saveString(this, SP.LOGIN_STATUS, SP.SP_TRUE)
                            Intent(this, BaseActivity::class.java)
                        }
                        nextIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(nextIntent)
                        finish()
                    } else {
                        // This case handles a server success but with missing data.
                        binding.btnContinue.isEnabled = true
                        Toast.makeText(this, response?.message ?: "An unknown error occurred.", Toast.LENGTH_LONG).show()
                    }
                }
                is NetworkState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnContinue.isEnabled = true
                    Toast.makeText(this, "Error: ${networkState.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ✅ NEW: Observer for the RESEND OTP request, to show the notification
        viewModel.loginState.observe(this) { state ->
            when(state) {
                is NetworkState.Loading -> {
                    Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show()
                }
                is NetworkState.Success -> {
                    val otp = state.data.otp
                    if (otp != null) {
                        Toast.makeText(this, "OTP resent successfully.", Toast.LENGTH_SHORT).show()
                        showOtpNotification(otp) // Show the notification with the new OTP
                    } else {
                        Toast.makeText(this, "Error: New OTP not received", Toast.LENGTH_SHORT).show()
                    }
                }
                is NetworkState.Error -> {
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun saveUserData(user: UserData) {
        SP.saveString(this, SP.USER_ID, user.userId)
        SP.saveString(this, SP.USER_MOBILE, user.mobileNumber)
        SP.saveString(this, SP.FULL_NAME, user.name)
        SP.saveString(this, SP.USER_NAME, user.userName)
        SP.saveString(this, SP.USER_EMAIL, user.emailId)
        SP.saveString(this, SP.USER_PROFILE_PIC, user.profilePic)
        SP.saveString(this, SP.USER_ABOUT_YOU, user.aboutYou)
        SP.saveString(this, SP.USER_COUNTRY_CODE, user.countryCode)
        SP.saveString(this, SP.USER_COUNTRY_SHORT_NAME, user.countryShortName)
        SP.saveString(this, SP.USER_IS_VERIFIED, user.isVerified.toString())
        SP.saveString(this, SP.USER_STATUS, user.status)
        SP.saveString(this, SP.USER_DELETED, user.deleted)
        SP.saveString(this, SP.USER_CREATED_AT, user.createdAt)
        SP.saveInterestNames(this, SP.USER_INTEREST_NAMES, user.interestNames)


        SP.saveBoolean(this, SP.IS_PAYOUT_VERIFIED, user.payoutMethodVerified)
        Log.d(TAG, "Saved IS_PAYOUT_VERIFIED: ${user.payoutMethodVerified}")

        // Save the display-friendly string provided by the server
        // Use a fallback message just in case
        val displayInfo = user.payoutInfoDisplay ?: "No payout method has been added."
        SP.saveString(this, SP.PAYOUT_INFO_DISPLAY, displayInfo)
        Log.d(TAG, "Saved PAYOUT_INFO_DISPLAY: $displayInfo")

    }

    private fun verifyOtp(mobile: String, otp: String) {
        binding.progressBar.visibility = View.VISIBLE // Show loading

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM token failed, proceeding without it.", task.exception)
                // If FCM fails, we still try to log in, but send a null token.
                sendVerificationRequest(mobile, otp, null)
                return@addOnCompleteListener
            }
            val fcmToken = task.result
            Log.d("FCM_TOKEN", "Got latest FCM token for verification: $fcmToken")
            sendVerificationRequest(mobile, otp, fcmToken)
        }
    }
    private fun sendVerificationRequest(mobile: String, otp: String, fcmToken: String?) {
        Log.d("FCM_TOKEN", "Sending FCM token to server: $fcmToken")

        // Gather the device details into a simple map for transport.
        val deviceDetails = mapOf(
            "device_id" to DeviceUtils.getDeviceId(this),
            "device_model" to DeviceUtils.getDeviceModel(),
            "os_version" to DeviceUtils.getOsVersion(),
            "app_version" to DeviceUtils.getAppVersion()
        )
        // Call the ViewModel with the clear, explicit parameters.
        viewModel.verifyOtp(mobile, otp, fcmToken, deviceDetails)
    }

    private fun showOtpNotification(otp: String) {
        val channelId = "otp_channel"
        val channelName = "OTP Notifications"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
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

        notificationManager.notify(102, notification) // Use a different ID to avoid conflicts
    }

    private fun showKeyboard(editText: EditText) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}