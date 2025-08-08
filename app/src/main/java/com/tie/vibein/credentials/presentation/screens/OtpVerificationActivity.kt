package com.tie.vibein.credentials.presentation.screens

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
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.BaseActivity
import com.tie.vibein.R
import com.tie.vibein.credentials.data.models.UserData
import com.tie.vibein.credentials.data.repository.AuthRepository
import com.tie.vibein.credentials.presentation.view_model.AuthViewModel
import com.tie.vibein.credentials.presentation.view_model.AuthViewModelFactory
import com.tie.vibein.databinding.ActivityOtpVerificationBinding
import com.tie.vibein.utils.EdgeToEdgeUtils
import com.tie.vibein.utils.NetworkState
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
            when (networkState) {
                is NetworkState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnContinue.isEnabled = false
                }
                is NetworkState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val user = networkState.data?.user
                    if (user != null) {
                        saveUserData(user)
                        Toast.makeText(this, "OTP Verified Successfully", Toast.LENGTH_SHORT).show()
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
                        binding.btnContinue.isEnabled = true
                        Toast.makeText(this, "Verification successful, but user data not found.", Toast.LENGTH_LONG).show()
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
        SP.saveString(this, SP.USER_ID, user.user_id)
        SP.saveString(this, SP.USER_MOBILE, user.mobile_number)
        SP.saveString(this, SP.FULL_NAME, user.name)
        SP.saveString(this, SP.USER_NAME, user.user_name)
        SP.saveString(this, SP.USER_EMAIL, user.email_id)
        SP.saveString(this, SP.USER_PROFILE_PIC, user.profile_pic)
        SP.saveString(this, SP.USER_ABOUT_YOU, user.about_you)
        SP.saveString(this, SP.USER_COUNTRY_CODE, user.country_code)
        SP.saveString(this, SP.USER_COUNTRY_SHORT_NAME, user.country_short_name)
        SP.saveString(this, SP.USER_IS_VERIFIED, user.is_verified.toString())
        SP.saveString(this, SP.USER_STATUS, user.status)
        SP.saveString(this, SP.USER_DELETED, user.deleted)
        SP.saveString(this, SP.USER_CREATED_AT, user.created_at)
        SP.saveInterestNames(this, SP.USER_INTEREST_NAMES, user.interest_names)



        SP.saveBoolean(this, SP.IS_PAYOUT_VERIFIED, user.payout_method_verified)
        Log.d(TAG, "Saved IS_PAYOUT_VERIFIED: ${user.payout_method_verified}")

        // Save the display-friendly string provided by the server
        // Use a fallback message just in case
        val displayInfo = user.payout_info_display ?: "No payout method has been added."
        SP.saveString(this, SP.PAYOUT_INFO_DISPLAY, displayInfo)
        Log.d(TAG, "Saved PAYOUT_INFO_DISPLAY: $displayInfo")

    }

    private fun verifyOtp(mobile: String, otp: String) {
        viewModel.verifyOtp(mobile, otp)
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