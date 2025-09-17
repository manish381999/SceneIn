package com.scenein.credentials.presentation.view_model

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenein.createEvent.data.models.ApiResponse
import com.scenein.credentials.data.models.LoginOtpResponse
import com.scenein.credentials.data.models.UsernameCheckResponse
import com.scenein.credentials.data.models.VerifyOtpResponse
import com.scenein.credentials.data.repository.CredentialRepository
import com.scenein.utils.NetworkState
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody

class CredentialViewModel : ViewModel() {

    private val repository = CredentialRepository()

    var isUsernameAvailable: Boolean = false
        private set

    private val _loginState = MutableLiveData<NetworkState<LoginOtpResponse>>()
    val loginState: LiveData<NetworkState<LoginOtpResponse>> = _loginState

    private val _verifyState = MutableLiveData<NetworkState<VerifyOtpResponse>>()
    val verifyState: LiveData<NetworkState<VerifyOtpResponse>> = _verifyState

    private val _updateUserState = MutableLiveData<NetworkState<VerifyOtpResponse>>()
    val updateUserState: LiveData<NetworkState<VerifyOtpResponse>> = _updateUserState

    private val _usernameCheckState = MutableLiveData<NetworkState<UsernameCheckResponse>?>()
    val usernameCheckState: LiveData<NetworkState<UsernameCheckResponse>?> get() = _usernameCheckState


    private val _removePicState = MutableLiveData<NetworkState<ApiResponse>>()
    val removePicState: LiveData<NetworkState<ApiResponse>> get() = _removePicState


    fun loginWithOtp(mobile: String, countryCode: String, countryShortName: String) {
        _loginState.value = NetworkState.Loading
        viewModelScope.launch {
            val result = repository.loginWithOtp(mobile, countryCode, countryShortName)
            _loginState.postValue(result)
        }
    }

    fun verifyOtp(
        mobile: String,
        otp: String,
        fcmToken: String?,
        deviceDetails: Map<String, String>
    ) {
        _verifyState.value = NetworkState.Loading
        viewModelScope.launch {
            val result = repository.verifyOtp(mobile, otp, fcmToken, deviceDetails)
            _verifyState.postValue(result)
        }
    }

    fun updateUser(
        name: RequestBody,
        userName: RequestBody,
        emailId: RequestBody,
        aboutYou: RequestBody,
        interest: RequestBody,
        profilePic: MultipartBody.Part?
    ) {
        _updateUserState.value = NetworkState.Loading
        viewModelScope.launch {
            val result = repository.updateUser(name, userName, emailId, aboutYou, interest, profilePic)
            _updateUserState.postValue(result)
        }
    }

    fun removeProfilePic() {
        _removePicState.value = NetworkState.Loading
        viewModelScope.launch {
            val result = repository.removeProfilePicture()
            _removePicState.postValue(result)
        }
    }

    fun checkUsername(userName: String) {
        _usernameCheckState.value = NetworkState.Loading
        isUsernameAvailable = false
        viewModelScope.launch {
            val result = repository.checkUsernameAvailability(userName)
            if (result is NetworkState.Success) {
                isUsernameAvailable = result.data.available
            }
            _usernameCheckState.postValue(result)
        }
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun clearUsernameCheckState() {
        _usernameCheckState.value = null
        isUsernameAvailable = false
    }
}