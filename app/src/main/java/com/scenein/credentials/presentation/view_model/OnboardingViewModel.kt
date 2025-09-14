package com.scenein.credentials.presentation.view_model

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OnboardingViewModel : ViewModel() {

    // --- Step 1 Data ---
    val fullName = MutableLiveData<String>()
    val userName = MutableLiveData<String>()
    val email = MutableLiveData<String>()

    // --- Step 2 Data ---
    val aboutYou = MutableLiveData<String>()
    val selectedImageUri = MutableLiveData<Uri?>()

    // --- Step 3 Data ---
    val selectedCategoryIdsString = MutableLiveData<String>()

    // --- Validation Error States ---
    val fullNameError = MutableLiveData<String?>()
    val userNameError = MutableLiveData<String?>()
    val emailError = MutableLiveData<String?>()
    val aboutYouError = MutableLiveData<String?>()
    val categoryError = MutableLiveData<String?>()

    fun validateStep1(isUsernameAvailable: Boolean): Boolean {
        var isValid = true

        if (fullName.value.isNullOrBlank()) {
            fullNameError.value = "Full name cannot be empty"
            isValid = false
        } else {
            fullNameError.value = null
        }

        if (userName.value.isNullOrBlank()) {
            userNameError.value = "Username cannot be empty"
            isValid = false
        } else if (!isUsernameAvailable) {
            userNameError.value = "This username is not available"
            isValid = false
        }
        else {
            userNameError.value = null
        }

        val emailValue = email.value
        if (emailValue.isNullOrBlank() || !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            emailError.value = "Please enter a valid email address"
            isValid = false
        } else {
            emailError.value = null
        }
        return isValid
    }

    fun validateStep2(): Boolean {
        if (aboutYou.value.isNullOrBlank()) {
            aboutYouError.value = "Please tell us a bit about yourself"
            return false
        } else {
            aboutYouError.value = null
            return true
        }
    }

    fun validateStep3(): Boolean {
        if (selectedCategoryIdsString.value.isNullOrBlank()) {
            categoryError.value = "Please select at least one interest"
            return false
        } else {
            categoryError.value = null
            return true
        }
    }
}