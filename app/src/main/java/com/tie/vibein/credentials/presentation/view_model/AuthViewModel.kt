package com.tie.vibein.credentials.presentation.view_model

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tie.vibein.createEvent.data.models.ApiResponse
import com.tie.vibein.credentials.data.models.LoginOtpResponse
import com.tie.vibein.credentials.data.models.UsernameCheckResponse
import com.tie.vibein.credentials.data.models.VerifyOtpResponse
import com.tie.vibein.credentials.data.repository.AuthRepository
import com.tie.vibein.utils.NetworkState
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableLiveData<NetworkState<LoginOtpResponse>>()
    val loginState: LiveData<NetworkState<LoginOtpResponse>> = _loginState

    private val _verifyState = MutableLiveData<NetworkState<VerifyOtpResponse>>()
    val verifyState: LiveData<NetworkState<VerifyOtpResponse>> = _verifyState

    private val _updateUserState = MutableLiveData<NetworkState<VerifyOtpResponse>>()
    val updateUserState: LiveData<NetworkState<VerifyOtpResponse>> = _updateUserState

    private val _fcmTokenState = MutableLiveData<NetworkState<String>>()
    val fcmTokenState: LiveData<NetworkState<String>> = _fcmTokenState

    private val _usernameCheckState = MutableLiveData<NetworkState<UsernameCheckResponse>>()
    val usernameCheckState: LiveData<NetworkState<UsernameCheckResponse>> get() = _usernameCheckState

    private val _removePicState = MutableLiveData<NetworkState<String>>()
    val removePicState: LiveData<NetworkState<String>> get() = _removePicState

    fun loginWithOtp(mobile: String, countryCode: String, countryShortName: String) {
        _loginState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.loginWithOtp(mobile, countryCode, countryShortName)
                if (response.isSuccessful && response.body() != null) {
                    _loginState.value = NetworkState.Success(response.body()!!)
                } else {
                    _loginState.value = NetworkState.Error(response.message())
                }
            } catch (e: Exception) {
                _loginState.value = NetworkState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun verifyOtp(mobile: String, otp: String) {
        _verifyState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.verifyOtp(mobile, otp)
                if (response.isSuccessful && response.body() != null) {
                    _verifyState.value = NetworkState.Success(response.body()!!)
                } else {
                    _verifyState.value = NetworkState.Error(response.message())
                }
            } catch (e: Exception) {
                _verifyState.value = NetworkState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun updateUser(
        userId: RequestBody,
        name: RequestBody,
        userName: RequestBody,
        emailId: RequestBody,
        aboutYou: RequestBody,
        interest: RequestBody,
        profilePic: MultipartBody.Part?
    ) {
        _updateUserState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.updateUser(userId, name, userName, emailId, aboutYou, interest, profilePic)
                if (response.isSuccessful && response.body() != null) {
                    _updateUserState.value = NetworkState.Success(response.body()!!)
                } else {
                    _updateUserState.value = NetworkState.Error(response.message())
                }
            } catch (e: Exception) {
                _updateUserState.value = NetworkState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun removeProfilePic(userId: String) {
        _removePicState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.removeProfilePicture(userId)
                if (response.isSuccessful) {
                    // We only need the message, so we extract it.
                    _removePicState.postValue(NetworkState.Success(response.body()!!.message))
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    _removePicState.postValue(NetworkState.Error(errorMsg))
                }
            } catch (e: Exception) {
                e.message?.let { _removePicState.postValue(NetworkState.Error(it)) }
            }
        }
    }

    fun checkUsername(userName: String, userId: String) {
        _usernameCheckState.value = NetworkState.Loading
        viewModelScope.launch {
            _usernameCheckState.value = try {
                val response = repository.checkUsernameAvailability(userName, userId)
                if (response.isSuccessful) {
                    NetworkState.Success(response.body()!!)
                } else {
                    NetworkState.Error(JSONObject(response.errorBody()!!.string()).getString("message"))
                }
            } catch (e: Exception) {
                e.message?.let { NetworkState.Error(it) }
            }
        }
    }

    // A function to reset the state so the check UI can be cleared.
    @SuppressLint("NullSafeMutableLiveData")
    fun clearUsernameCheckState() {
        _usernameCheckState.value = null
    }

    fun sendFcmToken(mobileNumber: String, fcmToken: String) {
        _fcmTokenState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.sendFcmToken(mobileNumber, fcmToken)

                val responseBody = response.body()
                val errorBody = response.errorBody()?.string()

                if (response.isSuccessful && responseBody != null) {
                    _fcmTokenState.value = NetworkState.Success(responseBody.message)
                } else {
                    // Now we show both error message and error body content
                    val errorMessage = responseBody?.message ?: errorBody ?: response.message()
                    _fcmTokenState.value = NetworkState.Error(errorMessage)
                }

            } catch (e: Exception) {
                _fcmTokenState.value = NetworkState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }



}
