package com.scenein.credentials.data.repository

import com.scenein.createEvent.data.models.ApiResponse
import com.scenein.credentials.data.models.LoginOtpResponse
import com.scenein.credentials.data.models.UsernameCheckResponse
import com.scenein.credentials.data.models.VerifyOtpResponse
import com.scenein.credentials.data.retrofit.RetrofitClient
import com.scenein.utils.NetworkState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject

class CredentialRepository {
    private val api = RetrofitClient.credentialApiEndPoint

    suspend fun loginWithOtp(mobile: String, countryCode: String, countryShortName: String): NetworkState<LoginOtpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.loginWithOtp(mobile, countryCode, countryShortName)
                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    suspend fun verifyOtp(
        mobile: String,
        otp: String,
        fcmToken: String?,
        deviceDetails: Map<String, String>
    ): NetworkState<VerifyOtpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.verifyOtp(
                    mobileNumber = mobile,
                    otp = otp,
                    deviceId = deviceDetails["device_id"]!!,
                    deviceModel = deviceDetails["device_model"]!!,
                    osVersion = deviceDetails["os_version"]!!,
                    appVersion = deviceDetails["app_version"]!!,
                    fcmToken = fcmToken
                )
                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    suspend fun updateUser(
        name: RequestBody,
        userName: RequestBody,
        emailId: RequestBody,
        aboutYou: RequestBody,
        interest: RequestBody,
        profilePic: MultipartBody.Part?
    ): NetworkState<VerifyOtpResponse> { // Assuming updateUser still returns the full user object. If not, change to GenericApiResponse
        return withContext(Dispatchers.IO) {
            try {
                val response = api.updateUser(name, userName, emailId, aboutYou, interest, profilePic)
                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }


    suspend fun removeProfilePicture(): NetworkState<ApiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.removeProfilePicture()
                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    suspend fun checkUsernameAvailability(userName: String): NetworkState<UsernameCheckResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.checkUsernameAvailability(userName)
                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }
}