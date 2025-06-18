package com.tie.vibein.credentials.data.retrofit

import com.tie.vibein.createEvent.data.models.ApiResponse
import com.tie.vibein.credentials.data.models.LoginOtpResponse
import com.tie.vibein.credentials.data.models.VerifyOtpResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @FormUrlEncoded
    @POST("api_v1/login_with_otp.php")
    suspend fun loginWithOtp(
        @Field("mobile_number") mobileNumber: String,
        @Field("country_code") countryCode: String,
        @Field("country_short_name") countryShortName: String
    ): Response<LoginOtpResponse>


    @FormUrlEncoded
    @POST("api_v1/verify_otp.php")
    suspend fun verifyOtp(
        @Field("mobile_number") mobileNumber: String,
        @Field("otp") otp: String
    ): Response<VerifyOtpResponse>

    @Multipart
    @POST("api_v1/update_user.php")
    suspend fun updateUser(
        @Part("user_id") userId: RequestBody,
        @Part("name") name: RequestBody,
        @Part("user_name") userName: RequestBody,
        @Part("email_id") emailId: RequestBody,
        @Part("about_you") aboutYou: RequestBody,
        @Part("interest") interest: RequestBody,
        @Part profilePic: MultipartBody.Part? // nullable if user doesn't upload a new photo
    ): Response<VerifyOtpResponse>

    @FormUrlEncoded
    @POST("api_v1/save_fcm_token.php")
    suspend fun sendFcmToken(
        @Field("mobile_number") mobileNumber: String,
        @Field("fcm_token") fcmToken: String
    ): Response<ApiResponse>

    @FormUrlEncoded
    @POST("api_v1/update_fcm_token.php")
    suspend fun updateFcmToken(
        @Field("user_id") userId: String,
        @Field("fcm_token") fcmToken: String
    ): Response<ApiResponse>

}