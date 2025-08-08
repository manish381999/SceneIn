package com.tie.vibein.credentials.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tie.dreamsquad.utils.SP
import com.tie.vibein.BaseActivity
import com.tie.vibein.R
import com.tie.vibein.createEvent.persentation.view_model.CreateEventViewModel
import com.tie.vibein.credentials.data.models.VerifyOtpResponse
import com.tie.vibein.databinding.ActivityOnboardingBinding
import com.tie.vibein.credentials.data.repository.AuthRepository
import com.tie.vibein.credentials.presentation.adapter.CategoryAdapter
import com.tie.vibein.credentials.presentation.view_model.AuthViewModel
import com.tie.vibein.credentials.presentation.view_model.AuthViewModelFactory
import com.tie.vibein.utils.EdgeToEdgeUtils
import com.tie.vibein.utils.FileUtils
import com.tie.vibein.utils.NetworkState
import com.tie.vibein.utils.EditPhotoBottomSheet
import com.tie.vibein.utils.dialogs.CustomAlertDialog
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val eventViewModel: CreateEventViewModel by viewModels()
    private lateinit var authViewModel: AuthViewModel

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private var selectedCategoryIdsString: String = ""
    private var isUsernameAvailable = true
    private val handler = Handler(Looper.getMainLooper())
    private var usernameCheckRunnable: Runnable? = null
    private lateinit var currentUserId: String

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { launchNativeCropper(uri) }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { cameraImageUri?.let { launchNativeCropper(it) } }
    }

    private val cropResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { croppedUri ->
                selectedImageUri = croppedUri
                Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(binding.ivProfile)
            }
        }
    }

    // --- THIS IS THE CRITICAL MISSING LAUNCHER for Camera Permission ---
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Permission was granted, now we can safely launch the camera.
            launchCamera()
        } else {
            // Permission was denied. Inform the user.
            Toast.makeText(this, "Camera permission is required to take a new photo.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getString(this, SP.USER_ID) ?: ""
        if (currentUserId.isEmpty()) { finish(); return }

        val factory = AuthViewModelFactory(AuthRepository())
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        setupListeners()
        setupObservers()
        eventViewModel.fetchCategories()
    }

    private fun setupListeners() {
        binding.rvInterested.layoutManager = GridLayoutManager(this, 3)
        binding.ivEdit.setOnClickListener { showEditPhotoBottomSheet() }
        binding.btnSubmit.setOnClickListener { validateAndSubmit() }

        // The username TextWatcher is complete and correct.
        binding.etUserName.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                usernameCheckRunnable?.let { handler.removeCallbacks(it) }
                usernameCheckRunnable = Runnable {
                    val username = s.toString().trim()
                    if (username.length >= 3) { authViewModel.checkUsername(username, currentUserId) }
                    else {
                        binding.usernameStatusIcon.isVisible = false
                        binding.usernameStatusProgress.isVisible = false
                        isUsernameAvailable = false
                        binding.tvUsernameStatusMessage.text = ""
                        authViewModel.clearUsernameCheckState()
                    }
                }
                handler.postDelayed(usernameCheckRunnable!!, 500)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { binding.etUserName.error = null }
        })

        addTextWatcher(binding.etFullName)
        addTextWatcher(binding.etEmail)
        addTextWatcher(binding.etAboutYou)
    }

    private fun setupObservers() {
        eventViewModel.categoryState.observe(this) { state ->
            if (state is NetworkState.Success) {
                binding.rvInterested.adapter = CategoryAdapter(
                    context = this,
                    categories = state.data.data,
                    initialSelectedIds = mutableListOf(), // or pass in actual selected IDs
                ) { selectedIds ->
                    selectedCategoryIdsString = selectedIds.joinToString(",")
                }
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Failed to load categories: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.usernameCheckState.observe(this) { state ->
            if (state == null) {
                binding.usernameStatusIcon.isVisible = false
                binding.usernameStatusProgress.isVisible = false
                binding.tvUsernameStatusMessage.text = ""
                return@observe
            }

            binding.usernameStatusProgress.isVisible = state is NetworkState.Loading
            binding.usernameStatusIcon.isVisible = state !is NetworkState.Loading
            binding.tvUsernameStatusMessage.isVisible = state !is NetworkState.Loading

            if (state is NetworkState.Success) {
                val response = state.data
                isUsernameAvailable = response.available
                val iconRes = if (isUsernameAvailable) R.drawable.ic_check_circle else R.drawable.ic_error
                val colorRes = if (isUsernameAvailable) R.color.colorSuccess else R.color.colorError
                binding.usernameStatusIcon.setImageResource(iconRes)
                binding.usernameStatusIcon.setColorFilter(ContextCompat.getColor(this, colorRes))
                binding.tvUsernameStatusMessage.text = response.message
                binding.tvUsernameStatusMessage.setTextColor(ContextCompat.getColor(this, colorRes))
            } else if (state is NetworkState.Error) {
                isUsernameAvailable = false
                binding.usernameStatusIcon.setImageResource(R.drawable.ic_error)
                binding.usernameStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.colorError))
                binding.tvUsernameStatusMessage.text = state.message
                binding.tvUsernameStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.colorError))
            }
        }

        authViewModel.updateUserState.observe(this) { state ->
            val isLoading = state is NetworkState.Loading
            binding.progressBar.isVisible = isLoading
            binding.btnSubmit.text = if (isLoading) "" else "Get Started"
            binding.btnSubmit.isEnabled = !isLoading
            when (state) {
                is NetworkState.Success -> {
                    Toast.makeText(this, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                    saveUserDataAndFinish(state.data)
                }
                is NetworkState.Error -> Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                else -> {}
            }
        }

        authViewModel.removePicState.observe(this) { state ->
            // You can show a loading indicator here if you want
            if (state is NetworkState.Success) {
                // When successful, update SharedPreferences and the UI.
                SP.saveString(this, SP.USER_PROFILE_PIC, null) // Remove the pic from local storage
                binding.ivProfile.setImageResource(R.drawable.ic_profile_placeholder) // Reset the UI
                Toast.makeText(this, state.data, Toast.LENGTH_SHORT).show()
                // Also update the main user data if needed or trigger a refresh in ProfileFragment
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
        }
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

    private fun validateAndSubmit() {
        val fullName = binding.etFullName.text.toString().trim()
        val userName = binding.etUserName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val about = binding.etAboutYou.text.toString().trim()
        var isValid = true

        if (fullName.isEmpty()) { binding.etFullName.error = "Full Name is required"; isValid = false }
        if (userName.isEmpty()) { binding.etUserName.error = "Username is required"; isValid = false }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.etEmail.error = "A valid email is required"; isValid = false }
        if (about.isEmpty()) { binding.etAboutYou.error = "Please tell us about yourself"; isValid = false }
        if (selectedCategoryIdsString.isEmpty()) { Toast.makeText(this, "Please select at least one interest", Toast.LENGTH_SHORT).show(); isValid = false }

        if (authViewModel.usernameCheckState.value is NetworkState.Loading) {
            Toast.makeText(this, "Verifying username, please wait...", Toast.LENGTH_SHORT).show(); isValid = false
        } else if (!isUsernameAvailable) {
            binding.etUserName.error = "This username is not available"; isValid = false
        }

        if (!isValid) return

        val profilePicPart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            FileUtils.getMultipartBodyPartFromUri(this, uri, "profile_pic")
        }

        authViewModel.updateUser(
            userId = currentUserId.toRequestBody("text/plain".toMediaType()),
            name = fullName.toRequestBody("text/plain".toMediaType()),
            userName = userName.toRequestBody("text/plain".toMediaType()),
            emailId = email.toRequestBody("text/plain".toMediaType()),
            aboutYou = about.toRequestBody("text/plain".toMediaType()),
            interest = selectedCategoryIdsString.toRequestBody("text/plain".toMediaType()),
            profilePic = profilePicPart
        )
    }

    private fun showEditPhotoBottomSheet() {
        val bottomSheet = EditPhotoBottomSheet.newInstance()
        bottomSheet.onCameraClick = {
            // --- THIS IS THE DEFINITIVE FIX: Check for permission BEFORE launching camera ---
            checkCameraPermissionAndLaunch()
        }
        bottomSheet.onGalleryClick = {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        bottomSheet.onRemoveClick = {
            CustomAlertDialog.show(
                context = this,
                title = "Remove Photo?",
                message = "Are you sure you want to remove your profile photo?",
                positiveButtonText = "Remove",
                onPositiveClick = {
                    // This now calls the correct ViewModel function
                    authViewModel.removeProfilePic(currentUserId)
                }
            )
        }
        bottomSheet.show(supportFragmentManager, "EditPhotoBottomSheet")
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // --- THIS IS THE FIX ---
                CustomAlertDialog.show(
                    context = this,
                    title = "Permission Needed",
                    message = "To take a new profile picture, SceneIn needs access to your camera.",
                    positiveButtonText = "OK",
                    onPositiveClick = {
                        // This code runs when the user taps "OK"
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
                // --- END OF FIX ---
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /** This helper function is ONLY called after permission has been granted. */
    private fun launchCamera() {
        cameraImageUri = createImageUri()
        if (cameraImageUri != null) {
            takePhotoLauncher.launch(cameraImageUri!!)
        } else {
            Toast.makeText(this, "Could not create a file for the photo.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchNativeCropper(uri: Uri) {
        val intent = Intent(this, CropActivity::class.java).apply {
            putExtra(CropActivity.EXTRA_IMAGE_URI, uri.toString())
        }
        cropResultLauncher.launch(intent)
    }

    private fun createImageUri(): Uri? {
        return try {
            val file = File(cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            val authority = "${applicationContext.packageName}.provider"
            FileProvider.getUriForFile(this, authority, file)
        } catch (e: Exception) {
            Log.e("OnboardingActivity", "Error creating image URI for camera", e)
            null
        }
    }


    private fun addTextWatcher(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { if (!s.isNullOrEmpty()) editText.error = null }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

}