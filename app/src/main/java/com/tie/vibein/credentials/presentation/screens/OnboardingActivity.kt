package com.tie.vibein.credentials.presentation.screens // Corrected package name based on file path

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri // --- ADD THIS IMPORT ---
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.BaseActivity
import com.tie.vibein.createEvent.persentation.view_model.CreateEventViewModel
import com.tie.vibein.credentials.data.models.VerifyOtpResponse
import com.tie.vibein.databinding.ActivityOnboardingBinding
import com.tie.vibein.utils.FileUtils
import com.tie.vibein.credentials.data.repository.AuthRepository
import com.tie.vibein.credentials.presentation.adapter.CategoryAdapter
import com.tie.vibein.credentials.presentation.view_model.AuthViewModel
import com.tie.vibein.credentials.presentation.view_model.AuthViewModelFactory
import com.tie.vibein.utils.NetworkState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val eventViewModel: CreateEventViewModel by viewModels()
    private lateinit var authViewModel: AuthViewModel

    // --- MODIFICATION 1: Store the URI, not the path string ---
    private var selectedImageUri: Uri? = null
    private var selectedCategoryIdsString: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authViewModel = ViewModelProvider(this, AuthViewModelFactory(AuthRepository()))[AuthViewModel::class.java]

        setupListeners()
        setupObservers()
        eventViewModel.fetchCategories()
    }

    private fun setupListeners() {
        binding.rvInterested.layoutManager = LinearLayoutManager(this)
        binding.ivEdit.setOnClickListener { openGallery() }
        binding.btnSubmit.setOnClickListener { validateAndSubmit() }
        addTextWatcher(binding.etFullName)
        addTextWatcher(binding.etUserName)
        addTextWatcher(binding.etEmail)
        addTextWatcher(binding.etEventDescription)
    }

    private fun setupObservers() {
        eventViewModel.categoryState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> {}
                is NetworkState.Success -> {
                    state.data.let { response ->
                        binding.rvInterested.adapter = CategoryAdapter(this, response.data) { selectedIds ->
                            selectedCategoryIdsString = selectedIds.joinToString(",")
                        }
                    }
                }
                is NetworkState.Error -> {
                    Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
            }
        }

        authViewModel.updateUserState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    Toast.makeText(this, "Updating profile...", Toast.LENGTH_SHORT).show()
                }
                is NetworkState.Success -> {
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                    saveUserDataToPreferences(state.data)
                }
                is NetworkState.Error -> {
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveUserDataToPreferences(response: VerifyOtpResponse) {
        val user = response.user
        user?.let {
            SP.saveString(this, SP.USER_ID, it.user_id)
            SP.saveString(this, SP.USER_MOBILE, user.mobile_number)
            SP.saveString(this, SP.FULL_NAME, user.name ?: "")
            SP.saveString(this, SP.USER_NAME, it.user_name ?: "")
            SP.saveString(this, SP.USER_EMAIL, it.email_id ?: "")
            SP.saveString(this, SP.USER_PROFILE_PIC, it.profile_pic ?: "")
            SP.saveString(this, SP.USER_ABOUT_YOU, user.about_you ?: "")
            SP.saveString(this, SP.USER_COUNTRY_CODE, it.country_code ?: "")
            SP.saveString(this, SP.USER_COUNTRY_SHORT_NAME, it.country_short_name ?: "")
            SP.saveBoolean(this, SP.USER_IS_VERIFIED, it.is_verified)
            SP.saveString(this, SP.USER_STATUS, it.status)
            SP.saveString(this, SP.USER_DELETED, it.deleted)
            SP.saveString(this, SP.USER_CREATED_AT, it.created_at)
            SP.saveInterestNames(this, SP.USER_INTEREST_NAMES, it.interest_names)
            SP.saveString(this, SP.LOGIN_STATUS, SP.SP_TRUE)

            SP.saveBoolean(this, SP.IS_PAYOUT_VERIFIED, user.payout_method_verified)
            Log.d(TAG, "Saved IS_PAYOUT_VERIFIED: ${user.payout_method_verified}")

            // Save the display-friendly string provided by the server
            // Use a fallback message just in case
            val displayInfo = user.payout_info_display ?: "No payout method has been added."
            SP.saveString(this, SP.PAYOUT_INFO_DISPLAY, displayInfo)
            Log.d(TAG, "Saved PAYOUT_INFO_DISPLAY: $displayInfo")

            Log.d("UserDataSaved", "All user data saved. Moving to BaseActivity.")
            val intent = Intent(this, BaseActivity::class.java)
            startActivity(intent)
            finish() // Finish OnboardingActivity so user can't go back
        }
    }

    private fun validateAndSubmit() {
        val fullName = binding.etFullName.text.toString().trim()
        val userName = binding.etUserName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val about = binding.etEventDescription.text.toString().trim()
        var isValid = true

        if (fullName.isEmpty()) {
            binding.etFullName.error = "Full Name is required"
            isValid = false
        }
        if (userName.isEmpty()) {
            binding.etUserName.error = "Username is required"
            isValid = false
        }
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            isValid = false
        }
        if (about.isEmpty()) {
            binding.etEventDescription.error = "Tell us about yourself"
            isValid = false
        }
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select a profile picture", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (!isValid) return

        submitProfile(fullName, userName, email, about)
    }

    private fun submitProfile(fullName: String, userName: String, email: String, about: String) {
        val userId = SP.getString(this, SP.USER_ID) ?: ""

        // --- MODIFICATION 3: Create the MultipartBody.Part directly from the stored URI ---
        val profilePicPart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            // "profile_pic" is the name your server API expects.
            FileUtils.getMultipartBodyPartFromUri(this, uri, "profile_pic")
        }

        authViewModel.updateUser(
            userId = userId.toRequestBody(),
            name = fullName.toRequestBody(),
            userName = userName.toRequestBody(),
            emailId = email.toRequestBody(),
            aboutYou = about.toRequestBody(),
            interest = selectedCategoryIdsString.toRequestBody(),
            profilePic = profilePicPart // Pass the newly created part
        )
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryResultLauncher.launch(intent)
    }

    // --- MODIFICATION 2: The launcher now just saves the URI and updates the UI ---
    private val galleryResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                selectedImageUri = it // Simply store the URI
                binding.ivProfile.setImageURI(it) // Update the image view
            } ?: Toast.makeText(this, "Failed to get image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addTextWatcher(editText: android.widget.EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { if (!s.isNullOrEmpty()) editText.error = null }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun String.toRequestBody(): RequestBody = this.toRequestBody("text/plain".toMediaType())
}