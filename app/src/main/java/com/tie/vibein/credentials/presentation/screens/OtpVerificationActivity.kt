package com.tie.vibein.credentials.presentation.screens

import android.content.Context
import android.content.Intent
import android.os.*
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.tie.dreamsquad.utils.SP
import com.tie.dreamsquad.utils.SP.savePreferences
import com.tie.vibein.BaseActivity
import com.tie.vibein.R
import com.tie.vibein.credentials.data.models.VerifyOtpResponse
import com.tie.vibein.credentials.data.repository.AuthRepository
import com.tie.vibein.credentials.presentation.view_model.AuthViewModel
import com.tie.vibein.credentials.presentation.view_model.AuthViewModelFactory
import com.tie.vibein.databinding.ActivityOtpVerificationBinding
import com.tie.vibein.utils.NetworkState

class OtpVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpVerificationBinding
    private lateinit var mobileNumber: String
    private lateinit var countryCode: String
    private lateinit var countryShortName: String
    private lateinit var otp: String

    private var countDownTimer: CountDownTimer? = null
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentData()

        val repository = AuthRepository()
        val viewModelFactory = AuthViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(AuthViewModel::class.java)

        initComponents()
        setupOtpInputs()
        onClickListener()
        observeViewModel()
    }

    private fun getIntentData() {
        mobileNumber = intent.getStringExtra("mobile_number") ?: ""
        countryCode = intent.getStringExtra("country_code") ?: ""
        countryShortName = intent.getStringExtra("country_short_name") ?: ""
    }

    private fun initComponents() {
        binding.tvSubtitle.text = "We have sent a verification code to $countryCode $mobileNumber"

        binding.etOtp1.requestFocus()
        Handler(Looper.getMainLooper()).postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etOtp1, InputMethodManager.SHOW_IMPLICIT)
        }, 300)

        startResendTimer()
    }

    private fun setupOtpInputs() {
        binding.etOtp1.addTextChangedListener { moveToNext(binding.etOtp1, binding.etOtp2) }
        binding.etOtp2.addTextChangedListener { moveToNext(binding.etOtp2, binding.etOtp3) }
        binding.etOtp3.addTextChangedListener { moveToNext(binding.etOtp3, binding.etOtp4) }
        binding.etOtp4.addTextChangedListener { moveToNext(binding.etOtp4, binding.etOtp5) }
        binding.etOtp5.addTextChangedListener { moveToNext(binding.etOtp5, binding.etOtp6) }

        binding.etOtp2.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && binding.etOtp2.text.isEmpty()) {
                binding.etOtp1.requestFocus()
                true
            } else false
        }

        binding.etOtp3.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && binding.etOtp3.text.isEmpty()) {
                binding.etOtp2.requestFocus()
                true
            } else false
        }

        binding.etOtp4.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && binding.etOtp4.text.isEmpty()) {
                binding.etOtp3.requestFocus()
                true
            } else false
        }

        binding.etOtp5.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && binding.etOtp5.text.isEmpty()) {
                binding.etOtp4.requestFocus()
                true
            } else false
        }

        binding.etOtp6.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && binding.etOtp6.text.isEmpty()) {
                binding.etOtp5.requestFocus()
                true
            } else false
        }

        binding.etOtp6.addTextChangedListener {
            val otpString = binding.etOtp1.text.toString() +
                    binding.etOtp2.text.toString() +
                    binding.etOtp3.text.toString() +
                    binding.etOtp4.text.toString() +
                    binding.etOtp5.text.toString() +
                    binding.etOtp6.text.toString()

            if (otpString.length == 6) {
                otp = otpString
                binding.btnContinue.isEnabled = true
                binding.btnContinue.setBackgroundResource(R.drawable.bg_login_btn_enable)

                // Hide keyboard after complete input
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.etOtp6.windowToken, 0)
            } else {
                binding.btnContinue.isEnabled = false
                binding.btnContinue.setBackgroundResource(R.drawable.bg_login_btn_disable)
            }
        }
    }

    private fun moveToNext(current: EditText, next: EditText) {
        if (current.text.length == 1) next.requestFocus()
    }

    private fun startResendTimer() {
        countDownTimer = object : CountDownTimer(30_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                binding.tvResendTimer.text = "Didn’t get the OTP? (Request again in 0:${secondsLeft}s)"
            }

            override fun onFinish() {
                binding.tvResendTimer.text = "Didn’t get the OTP?"
                binding.btnSms.isEnabled = true
                binding.btnWhatsapp.isEnabled = true
                binding.btnSms.setBackgroundResource(R.drawable.bg_login_btn_enable)
                binding.btnWhatsapp.setBackgroundResource(R.drawable.bg_login_btn_enable)
                binding.btnSms.setTextColor(ContextCompat.getColor(this@OtpVerificationActivity, android.R.color.black))
                binding.btnWhatsapp.setTextColor(ContextCompat.getColor(this@OtpVerificationActivity, android.R.color.black))
            }
        }.start()

        binding.btnSms.isEnabled = false
        binding.btnWhatsapp.isEnabled = false
    }

    private fun onClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.tvBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        binding.btnSms.setOnClickListener {
            if (binding.btnSms.isEnabled) {
                startResendTimer() // Restart the timer to prevent immediate repeat
                viewModel.loginWithOtp(mobileNumber, countryCode, countryShortName) // Resend OTP

                binding.btnSms.isEnabled = false
                binding.btnWhatsapp.isEnabled = false

                binding.btnSms.setBackgroundResource(R.drawable.bg_login_btn_disable)
                binding.btnWhatsapp.setBackgroundResource(R.drawable.bg_login_btn_disable)
                binding.btnSms.setTextColor(ContextCompat.getColor(this@OtpVerificationActivity, android.R.color.white))
                binding.btnWhatsapp.setTextColor(ContextCompat.getColor(this@OtpVerificationActivity, android.R.color.white))

                Toast.makeText(this, "OTP resent via SMS", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnWhatsapp.setOnClickListener {
            // You can implement WhatsApp resend logic here if needed
        }

        binding.btnContinue.setOnClickListener {
            verifyOtp(mobileNumber, otp)
        }
    }

    private fun observeViewModel() {
        viewModel.verifyState.observe(this) { networkState ->
            when (networkState) {
                is NetworkState.Loading -> {
                    // Show loading indicator
                }
                is NetworkState.Success -> {
                    val response = networkState.data as? VerifyOtpResponse
                    val user = response?.user

                    if (user != null) {
                        // Save user details in SharedPreferences using SP class
                        savePreferences(this, SP.USER_ID, user.user_id)
                        savePreferences(this, SP.USER_MOBILE, user.mobile_number)
                        savePreferences(this, SP.FULL_NAME, user.name ?: "")
                        savePreferences(this, SP.USER_NAME, user.user_name ?: "")
                        savePreferences(this, SP.USER_EMAIL, user.email_id ?: "")
                        savePreferences(this, SP.USER_PROFILE_PIC, user.profile_pic ?: "")
                        savePreferences(this, SP.USER_ABOUT_YOU, user.about_you ?: "")
                        savePreferences(this, SP.USER_COUNTRY_CODE, user.country_code ?: "")
                        savePreferences(this, SP.USER_COUNTRY_SHORT_NAME, user.country_short_name ?: "")
                        savePreferences(this, SP.USER_IS_VERIFIED, user.is_verified)
                        savePreferences(this, SP.USER_STATUS, user.status)
                        savePreferences(this, SP.USER_DELETED, user.deleted)
                        savePreferences(this, SP.USER_CREATED_AT, user.created_at)

                        // Save the interest names (list of strings) as a comma-separated string
                        SP.saveInterestNames(this, SP.USER_INTEREST_NAMES, user.interest_names)

                        Toast.makeText(this, "OTP Verified Successfully", Toast.LENGTH_SHORT).show()

                        // 🚀 Redirect based on whether user has a name
                        if (user.name.isNullOrEmpty()) {
                            val intent = Intent(this, OnboardingActivity::class.java)
                            startActivity(intent)
                        } else {
                            savePreferences(this, SP.LOGIN_STATUS, SP.SP_TRUE)
                            val intent = Intent(this, BaseActivity::class.java)
                            startActivity(intent)
                        }

                        finish()
                    }
                }
                is NetworkState.Error -> {
                    Toast.makeText(this, "Error: ${networkState.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun verifyOtp(mobile: String, otp: String) {
        viewModel.verifyOtp(mobile, otp)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
