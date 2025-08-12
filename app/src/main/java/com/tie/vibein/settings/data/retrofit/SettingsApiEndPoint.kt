package com.tie.vibein.settings.data.retrofit

import com.tie.vibein.settings.data.models.SettingsActionResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SettingsApiEndPoint {

    @FormUrlEncoded
    @POST("api_v1/update_privacy.php")
    suspend fun updatePrivacy(
        @Field("user_id") userId: String,
        @Field("is_private") isPrivate: Int // 1 for true, 0 for false
    ): Response<SettingsActionResponse>

    @FormUrlEncoded
    @POST("api_v1/users_deactivate_account.php")
    suspend fun deleteAccount(
        @Field("user_id") userId: String
    ): Response<SettingsActionResponse>

    @FormUrlEncoded
    @POST("api_v1/logout.php")
    suspend fun logout(
        @Field("user_id") userId: String
    ): Response<SettingsActionResponse>
}