package com.scenein.credentials.presentation.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.scenein.BaseActivity
import com.scenein.credentials.data.models.VerifyOtpResponse
import com.scenein.credentials.presentation.adapter.NUM_STEPS
import com.scenein.credentials.presentation.adapter.OnboardingPagerAdapter
import com.scenein.credentials.presentation.view_model.AuthViewModel
import com.scenein.credentials.presentation.view_model.OnboardingViewModel
import com.scenein.databinding.ActivityOnboardingBinding
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.FileUtils
import com.scenein.utils.NetworkState
import com.scenein.utils.SP
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- UPDATED: Use the interactive edge-to-edge utility ---
        binding.floatingActionContainer.post {
            EdgeToEdgeUtils.setUpInteractiveEdgeToEdge(
                rootView = binding.root,
                contentView = binding.viewPager,
                floatingView = binding.floatingActionContainer
            )
        }

        setupViewPager()
        setupNavigation()
        setupObservers()
        setupOnBackPressed()
    }

    private fun setupObservers() {
        authViewModel.updateUserState.observe(this) { state ->
            val isLoading = state is NetworkState.Loading
            // Now control the progress bar inside the button
            binding.finalProgressBar.isVisible = isLoading
            if (isLoading) {
                binding.btnNext.text = "" // Hide text when loading
            } else {
                // Restore text based on the current step
                binding.btnNext.text = if (binding.viewPager.currentItem == NUM_STEPS - 1) "Get Started" else "Next"
            }
            binding.btnNext.isEnabled = !isLoading
            binding.btnBack.isEnabled = !isLoading

            when (state) {
                is NetworkState.Success -> {
                    Toast.makeText(this, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                    saveUserDataAndFinish(state.data)
                }
                is NetworkState.Error -> {
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }


    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false // Prevent swiping
        binding.progressIndicator.max = NUM_STEPS

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.progressIndicator.progress = position + 1
                binding.btnBack.isVisible = position > 0
                binding.btnNext.text = if (position == NUM_STEPS - 1) "Get Started" else "Next"
            }
        })
    }

    private fun setupNavigation() {
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            var canProceed = false

            when (currentItem) {
                0 -> canProceed = onboardingViewModel.validateStep1(authViewModel.isUsernameAvailable)
                1 -> canProceed = onboardingViewModel.validateStep2()
                2 -> canProceed = onboardingViewModel.validateStep3()
            }

            if (canProceed) {
                if (currentItem < NUM_STEPS - 1) {
                    binding.viewPager.currentItem = currentItem + 1
                } else {
                    submitOnboardingData()
                }
            }
        }

        binding.btnBack.setOnClickListener {
            if (binding.viewPager.currentItem > 0) {
                binding.viewPager.currentItem = binding.viewPager.currentItem - 1
            }
        }
    }

    private fun submitOnboardingData() {
        val profilePicPart: MultipartBody.Part? = onboardingViewModel.selectedImageUri.value?.let { uri ->
            FileUtils.getMultipartBodyPartFromUri(this, uri, "profile_pic")
        }

        authViewModel.updateUser(
            name = onboardingViewModel.fullName.value!!.toRequestBody("text/plain".toMediaType()),
            userName = onboardingViewModel.userName.value!!.toRequestBody("text/plain".toMediaType()),
            emailId = onboardingViewModel.email.value!!.toRequestBody("text/plain".toMediaType()),
            aboutYou = onboardingViewModel.aboutYou.value!!.toRequestBody("text/plain".toMediaType()),
            interest = onboardingViewModel.selectedCategoryIdsString.value!!.toRequestBody("text/plain".toMediaType()),
            profilePic = profilePicPart
        )
    }

    private fun saveUserDataAndFinish(response: VerifyOtpResponse) {
        response.user?.let { user ->
            SP.saveString(this, SP.LOGIN_STATUS, SP.SP_TRUE)
            val intent = Intent(this, BaseActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewPager.currentItem == 0) {
                    finish()
                } else {
                    binding.viewPager.currentItem = binding.viewPager.currentItem - 1
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
}