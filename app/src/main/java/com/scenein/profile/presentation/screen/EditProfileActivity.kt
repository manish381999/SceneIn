package com.scenein.profile.presentation.screen

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
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.createEvent.data.models.Category
import com.scenein.createEvent.presentation.view_model.CreateEventViewModel
import com.scenein.credentials.presentation.adapter.CategoryAdapter
import com.scenein.credentials.presentation.screens.CropActivity
import com.scenein.credentials.presentation.view_model.CredentialViewModel
import com.scenein.databinding.ActivityEditProfileBinding
import com.scenein.profile.data.models.MyProfileData
import com.scenein.profile.presentation.view_model.ProfileViewModel
import com.scenein.utils.CustomAlertDialog
import com.scenein.utils.EdgeToEdgeUtils
import com.scenein.utils.EditPhotoBottomSheet
import com.scenein.utils.FileUtils
import com.scenein.utils.NetworkState
import com.scenein.utils.SP
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val profileViewModel: ProfileViewModel by viewModels()
    private val eventViewModel: CreateEventViewModel by viewModels()

    // --- UPDATED: ViewModel initialization is now consistent and factory-less ---
    private val credentialViewModel: CredentialViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var selectedCategoryIdsString: String = ""
    private var isUsernameAvailable: Boolean = true
    private lateinit var currentUserId: String
    private var originalUsername: String? = null
    private var allCategories: List<Category> = emptyList()

    private val handler = Handler(Looper.getMainLooper())
    private var usernameCheckRunnable: Runnable? = null

    // ... (ActivityResultLaunchers remain unchanged) ...
    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) launchNativeCropper(uri)
    }
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraImageUri?.let { launchNativeCropper(it) }
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
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCamera()
        else Toast.makeText(this, "Camera permission is required to take a photo.", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeUtils.setUpEdgeToEdge(this)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SP.getString(this, SP.USER_ID) ?: ""
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupListeners()
        setupObservers()
        profileViewModel.fetchMyProfile()
        eventViewModel.fetchCategories()
    }

    // ... (The rest of the file remains the same as its logic is already correct) ...

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvEditPhoto.setOnClickListener { showEditPhotoBottomSheet() }
        binding.btnSaveChanges.setOnClickListener { validateAndSubmit() }

        binding.etUserName.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                usernameCheckRunnable?.let { handler.removeCallbacks(it) }
                usernameCheckRunnable = Runnable {
                    val username = s.toString().trim()
                    if (username.isNotEmpty() && username.lowercase() != originalUsername?.lowercase() && username.length >= 3) {
                        credentialViewModel.checkUsername(username)
                    } else {
                        isUsernameAvailable = (username.lowercase() == originalUsername?.lowercase())
                        credentialViewModel.clearUsernameCheckState()
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
        profileViewModel.myProfileState.observe(this) { state ->
            if (state is NetworkState.Success) {
                populateForm(state.data)
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Error fetching profile: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }

        eventViewModel.categoryState.observe(this) { state ->
            if (state is NetworkState.Success) {
                allCategories = state.data.data
                (profileViewModel.myProfileState.value as? NetworkState.Success)?.data?.let {
                    populateForm(it)
                }
            }
        }

        credentialViewModel.usernameCheckState.observe(this) { state ->
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
                isUsernameAvailable = state.data.available
                val iconRes = if (isUsernameAvailable) R.drawable.ic_check_circle else R.drawable.ic_error
                val colorRes = if (isUsernameAvailable) R.color.colorSuccess else R.color.colorError
                binding.usernameStatusIcon.setImageResource(iconRes)
                binding.usernameStatusIcon.setColorFilter(ContextCompat.getColor(this, colorRes))
                binding.tvUsernameStatusMessage.text = state.data.message
                binding.tvUsernameStatusMessage.setTextColor(ContextCompat.getColor(this, colorRes))
            } else if (state is NetworkState.Error) {
                isUsernameAvailable = false
                binding.usernameStatusIcon.setImageResource(R.drawable.ic_error)
                binding.usernameStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.colorError))
                binding.tvUsernameStatusMessage.text = state.message
                binding.tvUsernameStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.colorError))
            }
        }

        credentialViewModel.updateUserState.observe(this) { state ->
            val isLoading = state is NetworkState.Loading
            binding.progressBar.isVisible = isLoading
            binding.btnSaveChanges.text = if (isLoading) "" else "Save Changes"
            binding.btnSaveChanges.isEnabled = !isLoading
            if (state is NetworkState.Success) {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
        }

        credentialViewModel.removePicState.observe(this) { state ->
            if (state is NetworkState.Success) {
                SP.saveString(this, SP.USER_PROFILE_PIC, null)
                binding.ivProfile.setImageResource(R.drawable.ic_profile_placeholder)
                Toast.makeText(this, "Photo removed.", Toast.LENGTH_SHORT).show()
            } else if (state is NetworkState.Error) {
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun populateForm(user: MyProfileData) {
        originalUsername = user.userName
        binding.etFullName.setText(user.name)
        binding.etUserName.setText(user.userName)
        binding.etEmail.setText(user.emailId)
        binding.etAboutYou.setText(user.aboutYou)
        Glide.with(this).load(user.profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.ivProfile)

        if (allCategories.isNotEmpty()) {
            val userInterestNames = user.interestNames
            val initialSelectionIds = allCategories
                .filter { userInterestNames.contains(it.category_name) }
                .map { it.id }
                .toMutableList()
            selectedCategoryIdsString = initialSelectionIds.joinToString(",")
            binding.rvInterests.layoutManager = GridLayoutManager(this, 3)
            binding.rvInterests.adapter = CategoryAdapter(this, allCategories, initialSelectionIds) { selectedIds ->
                selectedCategoryIdsString = selectedIds.joinToString(",")
            }
        }
    }

    private fun validateAndSubmit() {
        val fullName = binding.etFullName.text.toString().trim()
        val userName = binding.etUserName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val about = binding.etAboutYou.text.toString().trim()
        var isValid = true

        if (fullName.isEmpty()) { binding.etFullName.error = "Required"; isValid = false }
        if (userName.isEmpty()) { binding.etUserName.error = "Required"; isValid = false }
        if (email.isEmpty()) { binding.etEmail.error = "Required"; isValid = false }
        if (about.isEmpty()) { binding.etAboutYou.error = "Required"; isValid = false }
        if (userName.lowercase() != originalUsername?.lowercase() && !isUsernameAvailable) {
            Toast.makeText(this, "This username is not available.", Toast.LENGTH_SHORT).show(); isValid = false
        }
        if (!isValid) return

        val profilePicPart: MultipartBody.Part? = selectedImageUri?.let {
            FileUtils.getMultipartBodyPartFromUri(this, it, "profile_pic")
        }

        credentialViewModel.updateUser(
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
        bottomSheet.onCameraClick = { checkCameraPermissionAndLaunch() }
        bottomSheet.onGalleryClick = { pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        bottomSheet.onRemoveClick = {
            CustomAlertDialog.show(
                context = this,
                title = "Remove Photo?",
                message = "Are you sure you want to remove your profile photo?",
                positiveButtonText = "Remove",
                onPositiveClick = {
                    credentialViewModel.removeProfilePic()
                }
            )
        }
        bottomSheet.show(supportFragmentManager, "EditPhotoBottomSheet")
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> launchCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                CustomAlertDialog.show(
                    this, "Permission Needed", "SceneIn needs camera access to take a profile picture.", "OK",
                    onPositiveClick = { requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        cameraImageUri = createImageUri()
        if (cameraImageUri != null) {
            takePhotoLauncher.launch(cameraImageUri!!)
        } else {
            Toast.makeText(this, "Could not create file for photo.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchNativeCropper(uri: Uri) {
        val intent = Intent(this, CropActivity::class.java)
        intent.putExtra(CropActivity.EXTRA_IMAGE_URI, uri.toString())
        cropResultLauncher.launch(intent)
    }

    private fun createImageUri(): Uri? {
        return try {
            val file = File(cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            val authority = "${applicationContext.packageName}.provider"
            FileProvider.getUriForFile(this, authority, file)
        } catch (e: Exception) {
            Log.e("EditProfileActivity", "Error creating image URI for camera", e); null
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