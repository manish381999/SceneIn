package com.scenein.credentials.data.repository


import com.scenein.credentials.data.models.LoginOtpResponse
import com.scenein.credentials.data.models.VerifyOtpResponse
import com.scenein.credentials.data.retrofit.RetrofitClient
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
        name: RequestBody,
        userName: RequestBody,
        emailId: RequestBody,
        aboutYou: RequestBody,
        interest: RequestBody,
        profilePic: MultipartBody.Part?
    ): Response<VerifyOtpResponse> {
        return api.updateUser(name, userName, emailId, aboutYou, interest, profilePic)
    }

    suspend fun removeProfilePicture() = api.removeProfilePicture()

    suspend fun checkUsernameAvailability(userName: String) =
        api.checkUsernameAvailability(userName)

}