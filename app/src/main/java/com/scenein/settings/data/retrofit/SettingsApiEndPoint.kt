package com.scenein.settings.data.retrofit

import com.scenein.discover.data.models.DiscoverApiResponse
import com.scenein.settings.data.models.SettingsActionResponse
import com.scenein.settings.data.models.TransactionDetailResponse
import com.scenein.settings.data.models.TransactionHistoryResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface SettingsApiEndPoint {



    @GET("transactions/ticket_history") // No user_id needed in body due to auth_check
    suspend fun getTicketTransactionHistory(): Response<TransactionHistoryResponse>


    @FormUrlEncoded
    @POST("transactions/ticket_details")
    suspend fun getTransactionDetails(
        @Field("transaction_id") transactionId: Int
    ): Response<TransactionDetailResponse>


    @FormUrlEncoded
    @POST("users/update_privacy")
    suspend fun updatePrivacy(
        @Field("is_private") isPrivate: Int // 1 for true, 0 for false
    ): Response<SettingsActionResponse>


    @POST("users/deactivate_account")
    suspend fun deleteAccount(
    ): Response<SettingsActionResponse>


    @POST("auth/logout")
    suspend fun logout(
    ): Response<SettingsActionResponse>


    @FormUrlEncoded
    @POST("bookmarks/get_bookmarked")
    suspend fun getBookmarkedEvents(@Field("page") page: Int): Response<DiscoverApiResponse>
}