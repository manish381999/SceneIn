package com.scenein.credentials.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.scenein.R
import com.scenein.credentials.presentation.view_model.CredentialViewModel
import com.scenein.credentials.presentation.view_model.OnboardingViewModel
import com.scenein.databinding.FragmentOnboardingStep2Binding
import com.scenein.utils.CustomAlertDialog
import com.scenein.utils.EditPhotoBottomSheet
import java.io.File

class OnboardingStep2Fragment : Fragment(R.layout.fragment_onboarding_step2) {

    private var _binding: FragmentOnboardingStep2Binding? = null
    private val binding get() = _binding!!

    private val onboardingViewModel: OnboardingViewModel by activityViewModels()
    private val credentialViewModel: CredentialViewModel by activityViewModels()

    private var cameraImageUri: Uri? = null

    // --- ActivityResultLaunchers ---
    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { launchNativeCropper(uri) }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { cameraImageUri?.let { launchNativeCropper(it) } }
    }

    private val cropResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { croppedUri ->
                onboardingViewModel.selectedImageUri.value = croppedUri
            }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingStep2Binding.bind(view)

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.fabEditPhoto.setOnClickListener { showEditPhotoBottomSheet() }
        binding.etAboutYou.doAfterTextChanged {
            onboardingViewModel.aboutYou.value = it.toString().trim()
            // THE FIX: Instead of hiding the view, clear the error.
            binding.tilAboutYou.error = null
        }
    }

    private fun setupObservers() {
        onboardingViewModel.selectedImageUri.observe(viewLifecycleOwner) { uri ->
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(binding.ivProfile)
        }

        onboardingViewModel.aboutYouError.observe(viewLifecycleOwner) { error ->
            binding.tilAboutYou.error = error
        }

        credentialViewModel.removePicState.observe(viewLifecycleOwner) { state ->
            // You can also handle loading/error states for pic removal
        }
    }

    private fun showEditPhotoBottomSheet() {
        val bottomSheet = EditPhotoBottomSheet.newInstance()
        bottomSheet.onCameraClick = { checkCameraPermissionAndLaunch() }
        bottomSheet.onGalleryClick = { pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        bottomSheet.onRemoveClick = {
            CustomAlertDialog.show(
                context = requireContext(),
                title = "Remove Photo?",
                message = "Are you sure you want to remove your profile photo?",
                positiveButtonText = "Remove",
                onPositiveClick = {
                    onboardingViewModel.selectedImageUri.value = null
                    // If a user can have a profile picture already, you would call the API here:
                    // authViewModel.removeProfilePic()
                }
            )
        }
        bottomSheet.show(childFragmentManager, "EditPhotoBottomSheet")
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> launchCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                CustomAlertDialog.show(
                    context = requireContext(),
                    title = "Permission Needed",
                    message = "To take a new photo, this app needs access to your camera.",
                    positiveButtonText = "OK",
                    onPositiveClick = { requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        createImageUri()?.let {
            cameraImageUri = it
            takePhotoLauncher.launch(it)
        }
    }

    private fun launchNativeCropper(uri: Uri) {
        // You will need to create this CropActivity
        val intent = Intent(requireContext(), CropActivity::class.java).apply {
            putExtra(CropActivity.EXTRA_IMAGE_URI, uri.toString())
        }
        cropResultLauncher.launch(intent)
    }

    private fun createImageUri(): Uri? {
        return try {
            val file = File(requireContext().cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            val authority = "${requireContext().applicationContext.packageName}.provider"
            FileProvider.getUriForFile(requireContext(), authority, file)
        } catch (e: Exception) {
            Log.e("OnboardingStep2", "Error creating image URI for camera", e)
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}