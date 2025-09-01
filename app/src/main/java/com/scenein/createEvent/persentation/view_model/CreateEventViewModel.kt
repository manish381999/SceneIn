package com.scenein.createEvent.persentation.view_model

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenein.createEvent.data.models.AgeRestrictionResponse
import com.scenein.createEvent.data.models.ApiResponse
import com.scenein.createEvent.data.models.GetCategoryResponse
import com.scenein.createEvent.data.repository.CreateEventRepository
import com.scenein.utils.NetworkState
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody

class CreateEventViewModel : ViewModel() {

    private val repository = CreateEventRepository()

    // --- LiveData for each form field ---
    val eventName = MutableLiveData<String>()
    val selectedCategory = MutableLiveData<Pair<String, String>?>()
    val eventDescription = MutableLiveData<String>()
    val eventDate = MutableLiveData<String>()
    val startTime = MutableLiveData<String>()
    val endTime = MutableLiveData<String>()
    val locationMode = MutableLiveData("In-Person")

    // --- UPDATED PROPERTIES FOR LOCATION ---
    val venueName = MutableLiveData<String>() // For the simple venue name
    val fullAddress = MutableLiveData<String>() // For the complete address
    val city = MutableLiveData<String>()
    val pincode = MutableLiveData<String>()
    val latitude = MutableLiveData<Double>()
    val longitude = MutableLiveData<Double>()

    val meetingLink = MutableLiveData<String>()
    val maxParticipants = MutableLiveData<String>()
    val selectedAge = MutableLiveData<Pair<String, String>?>()
    val eventType = MutableLiveData("Free")
    val ticketPrice = MutableLiveData<String>()
    val coverImageUri = MutableLiveData<Uri>()

    private val _isStep1Valid = MutableLiveData(false)
    val isStep1Valid: LiveData<Boolean> get() = _isStep1Valid

    private val _isStep2Valid = MutableLiveData(false)
    val isStep2Valid: LiveData<Boolean> get() = _isStep2Valid

    private val _isStep3Valid = MutableLiveData(false)
    val isStep3Valid: LiveData<Boolean> get() = _isStep3Valid

    private val _isStep4Valid = MutableLiveData(false)
    val isStep4Valid: LiveData<Boolean> get() = _isStep4Valid

    fun checkAllValidations() {
        val isEventNameValid = (eventName.value?.length ?: 0) >= 3
        val isCategoryValid = selectedCategory.value != null
        val isDescriptionValid = (eventDescription.value?.length ?: 0) >= 10
        _isStep1Valid.value = isEventNameValid && isCategoryValid && isDescriptionValid

        val isDateValid = !eventDate.value.isNullOrEmpty()
        val isStartTimeValid = !startTime.value.isNullOrEmpty()
        val isLocationValid = if (locationMode.value == "In-Person") {
            !fullAddress.value.isNullOrEmpty() && latitude.value != null && longitude.value != null
        } else {
            android.util.Patterns.WEB_URL.matcher(meetingLink.value ?: "").matches()
        }
        _isStep2Valid.value = isDateValid && isStartTimeValid && isLocationValid

        val isMaxParticipantsValid = (maxParticipants.value?.toIntOrNull() ?: 0) > 0
        val isAgeValid = selectedAge.value != null
        val isPricingValid = if (eventType.value == "Paid") {
            (ticketPrice.value?.toDoubleOrNull() ?: 0.0) > 0
        } else { true }
        _isStep3Valid.value = isMaxParticipantsValid && isAgeValid && isPricingValid

        _isStep4Valid.value = coverImageUri.value != null
    }

    private val _categoryState = MutableLiveData<NetworkState<GetCategoryResponse>>()
    val categoryState: LiveData<NetworkState<GetCategoryResponse>> get() = _categoryState
    private val _ageRestrictionState = MutableLiveData<NetworkState<AgeRestrictionResponse>>()
    val ageRestrictionState: LiveData<NetworkState<AgeRestrictionResponse>> get() = _ageRestrictionState
    private val _createEventState = MutableLiveData<NetworkState<ApiResponse>>()
    val createEventState: LiveData<NetworkState<ApiResponse>> get() = _createEventState

    fun fetchCategories() {
        _categoryState.value = NetworkState.Loading
        viewModelScope.launch {
            val result = repository.fetchCategories()
            _categoryState.postValue(result)
        }
    }
    fun fetchAgeRestrictions() {
        _ageRestrictionState.value = NetworkState.Loading
        viewModelScope.launch {
            val result = repository.fetchAgeRestrictions()
            _ageRestrictionState.postValue(result)
        }
    }
    fun createEvent(params: Map<String, RequestBody>, cover_image: MultipartBody.Part?) {
        _createEventState.value = NetworkState.Loading
        viewModelScope.launch {
            val result = repository.createEvent(params, cover_image)
            _createEventState.postValue(result)
        }
    }
}