package com.tie.vibein.createEvent.persentation.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tie.vibein.createEvent.data.models.AgeRestrictionResponse
import com.tie.vibein.createEvent.data.models.ApiResponse
import com.tie.vibein.createEvent.data.models.GetCategoryResponse
import com.tie.vibein.createEvent.data.repository.CreateEventRepository
import com.tie.vibein.utils.NetworkState
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody

class CreateEventViewModel : ViewModel() {

    private val repository = CreateEventRepository()

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

    fun createEvent(
        params: Map<String, RequestBody>,
        cover_image: MultipartBody.Part?
    ) {
        _createEventState.value = NetworkState.Loading
        viewModelScope.launch {
            val result = repository.createEvent(params, cover_image)
            _createEventState.postValue(result)
        }
    }
}
