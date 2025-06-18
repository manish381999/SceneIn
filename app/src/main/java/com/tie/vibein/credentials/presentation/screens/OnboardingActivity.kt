package com.tie.vibein.ui

import android.app.Activity
import android.content.Intent
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val eventViewModel: CreateEventViewModel by viewModels()
    private lateinit var authViewModel: AuthViewModel

    private var selectedImagePath: String? = null
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

        binding.ivEdit.setOnClickListener {
            openGallery()
        }

        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }

        // Add Text Watcher for validation
        addTextWatcher(binding.etFullName)
        addTextWatcher(binding.etUserName)
        addTextWatcher(binding.etEmail)
        addTextWatcher(binding.etEventDescription)
    }

    private fun setupObservers() {
        eventViewModel.categoryState.observe(this) { state ->
            when (state) {
                is NetworkState.Loading -> {
                    // Optionally, show loading spinner
                }
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

                    // Save user data to SharedPreferences
                    state.data.let { response ->
                        saveUserDataToPreferences(response)
                    }
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
            // Save individual data to SharedPreferences
            SP.savePreferences(this, SP.USER_ID, it.user_id)
            SP.savePreferences(this, SP.USER_MOBILE, user.mobile_number)
            SP.savePreferences(this, SP.FULL_NAME, user.name ?: "")
            SP.savePreferences(this, SP.USER_NAME, it.user_name ?: "")
            SP.savePreferences(this, SP.USER_EMAIL, it.email_id ?: "")
            SP.savePreferences(this, SP.USER_PROFILE_PIC, it.profile_pic ?: "")
            SP.savePreferences(this, SP.USER_ABOUT_YOU, user.about_you ?: "")
            SP.savePreferences(this, SP.USER_COUNTRY_CODE, it.country_code ?: "")
            SP.savePreferences(this, SP.USER_COUNTRY_SHORT_NAME, it.country_short_name ?: "")
            SP.savePreferences(this, SP.USER_IS_VERIFIED, it.is_verified)
            SP.savePreferences(this, SP.USER_STATUS, it.status)
            SP.savePreferences(this, SP.USER_DELETED, it.deleted)
            SP.savePreferences(this, SP.USER_CREATED_AT, it.created_at)

            // Save list of interests
            SP.saveInterestNames(this, SP.USER_INTEREST_NAMES, it.interest_names)

            // Save login status as true
            SP.savePreferences(this, SP.LOGIN_STATUS, SP.SP_TRUE)

            // Log all saved preferences
            Log.d("UserDataSaved", "User ID: ${it.user_id}")
            Log.d("UserDataSaved", "Mobile Number: ${user.mobile_number}")
            Log.d("UserDataSaved", "Full Name: ${user.name ?: ""}")
            Log.d("UserDataSaved", "User Name: ${it.user_name ?: ""}")
            Log.d("UserDataSaved", "Email: ${it.email_id ?: ""}")
            Log.d("UserDataSaved", "Profile Pic URL: ${it.profile_pic ?: ""}")
            Log.d("UserDataSaved", "About You: ${user.about_you ?: ""}")
            Log.d("UserDataSaved", "Country Code: ${it.country_code ?: ""}")
            Log.d("UserDataSaved", "Country Short Name: ${it.country_short_name ?: ""}")
            Log.d("UserDataSaved", "Is Verified: ${it.is_verified}")
            Log.d("UserDataSaved", "Status: ${it.status}")
            Log.d("UserDataSaved", "Deleted: ${it.deleted}")
            Log.d("UserDataSaved", "Created At: ${it.created_at}")
            Log.d("UserDataSaved", "Interest Names: ${it.interest_names.joinToString(", ")}")

            // Move to BaseActivity
            val intent = Intent(this, BaseActivity::class.java)
            startActivity(intent)
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

        if (!isValid) return

        submitProfile(fullName, userName, email, about)
    }

    private fun submitProfile(fullName: String, userName: String, email: String, about: String) {
        val userId = SP.getPreferences(this, SP.USER_ID) ?: ""
        val userIdBody = userId.toRequestBody()
        val nameBody = fullName.toRequestBody()
        val userNameBody = userName.toRequestBody()
        val emailBody = email.toRequestBody()
        val aboutBody = about.toRequestBody()
        val interestBody = selectedCategoryIdsString.toRequestBody()

        val profile_pic = selectedImagePath?.let {
            val file = File(it)
            val mediaType = "image/*".toMediaTypeOrNull()
            val requestFile = RequestBody.create(mediaType, file)
            MultipartBody.Part.createFormData("profile_pic", file.name, requestFile)
        }

        authViewModel.updateUser(
            userId = userIdBody,
            name = nameBody,
            userName = userNameBody,
            emailId = emailBody,
            aboutYou = aboutBody,
            interest = interestBody,
            profilePic = profile_pic
        )
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryResultLauncher.launch(intent)
    }

    private val galleryResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                val file = FileUtils.getFileFromUri(this, it)
                file?.let {
                    selectedImagePath = it.absolutePath
                    binding.ivProfile.setImageURI(uri)
                } ?: Toast.makeText(this, "Unable to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addTextWatcher(editText: android.widget.EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) editText.error = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // Extension function to convert a String to RequestBody
    private fun String.toRequestBody(): RequestBody =
        this.toRequestBody("text/plain".toMediaType())
}
