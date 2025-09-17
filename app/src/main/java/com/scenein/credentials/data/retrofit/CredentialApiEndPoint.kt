package com.scenein.credentials.data.retrofit


import com.scenein.createEvent.data.models.ApiResponse
import com.scenein.credentials.data.models.LoginOtpResponse
import com.scenein.credentials.data.models.UsernameCheckResponse
import com.scenein.credentials.data.models.VerifyOtpResponse
import com.scenein.tickets.data.models.GenericApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CredentialApiEndPoint {
    @FormUrlEncoded
    @POST("auth/login_with_otp")
    suspend fun loginWithOtp(
        @Field("mobile_number") mobileNumber: String,
        @Field("country_code") countryCode: String,
        @Field("country_short_name") countryShortName: String
    ): Response<LoginOtpResponse>


    @FormUrlEncoded
    @POST("auth/verify_otp")
    suspend fun verifyOtp(
        @Field("mobile_number") mobileNumber: String,
        @Field("otp") otp: String,
        @Field("device_id") deviceId: String,
        @Field("device_model") deviceModel: String,
        @Field("os_version") osVersion: String,
        @Field("app_version") appVersion: String,
        @Field("fcm_token") fcmToken: String?
    ): Response<VerifyOtpResponse>

    @Multipart
    @POST("users/update_profile")
    suspend fun updateUser(
        @Part("name") name: RequestBody,
        @Part("user_name") userName: RequestBody,
        @Part("email_id") emailId: RequestBody,
        @Part("about_you") aboutYou: RequestBody,
        @Part("interest") interest: RequestBody,
        @Part profilePic: MultipartBody.Part? // nullable if user doesn't upload a new photo
    ): Response<VerifyOtpResponse>

    @POST("users/remove_profile_picture")
    suspend fun removeProfilePicture(): Response<ApiResponse>

    @FormUrlEncoded
    @POST("users/check_username")
    suspend fun checkUsernameAvailability(
        @Field("user_name") userName: String
    ): Response<UsernameCheckResponse>


}