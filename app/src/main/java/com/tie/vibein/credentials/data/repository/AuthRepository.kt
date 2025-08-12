package com.tie.vibein.credentials.data.repository

import com.tie.vibein.createEvent.data.models.ApiResponse
import com.tie.vibein.credentials.data.models.LoginOtpResponse
import com.tie.vibein.credentials.data.models.VerifyOtpResponse
import com.tie.vibein.credentials.data.retrofit.RetrofitClient
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

class AuthRepository {
    private val api = RetrofitClient.apiService

    suspend fun loginWithOtp(mobile: String, countryCode: String, countryShortName: String): Response<LoginOtpResponse> {
        return api.loginWithOtp(mobile, countryCode, countryShortName)
    }


    suspend fun verifyOtp(
        mobile: String,
        otp: String,
        fcmToken: String?,
        deviceDetails: Map<String, String>
    ): Response<VerifyOtpResponse> {
        return api.verifyOtp(
            mobileNumber = mobile,
            otp = otp,
            deviceId = deviceDetails["device_id"]!!,
            deviceModel = deviceDetails["device_model"]!!,
            osVersion = deviceDetails["os_version"]!!,
            appVersion = deviceDetails["app_version"]!!,
            fcmToken = fcmToken
        )
    }

    suspend fun updateUser(
        userId: RequestBody,
        name: RequestBody,
        userName: RequestBody,
        emailId: RequestBody,
        aboutYou: RequestBody,
        interest: RequestBody,
        profilePic: MultipartBody.Part?
    ): Response<VerifyOtpResponse> {
        return api.updateUser(userId, name, userName, emailId, aboutYou, interest, profilePic)
    }

    suspend fun removeProfilePicture(userId: String) = api.removeProfilePicture(userId)

    suspend fun checkUsernameAvailability(userName: String, userId: String) =
        api.checkUsernameAvailability(userName, userId)

    suspend fun sendFcmToken(mobileNumber: String, fcmToken: String): Response<ApiResponse> {
        return api.sendFcmToken(mobileNumber, fcmToken)
    }

    suspend fun updateFcmToken(userId: String, fcmToken: String): Response<ApiResponse> {
        return api.updateFcmToken(userId, fcmToken)
    }

}