package com.scenein.settings.data.retrofit

import com.scenein.discover.data.models.DiscoverApiResponse
import com.scenein.settings.data.models.SettingsActionResponse
import com.scenein.settings.data.models.TransactionDetailResponse
import com.scenein.settings.data.models.TransactionHistoryResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SettingsApiEndPoint {



    @POST("api_v1/ticket_transaction_history.php") // No user_id needed in body due to auth_check
    suspend fun getTicketTransactionHistory(): Response<TransactionHistoryResponse>

    @FormUrlEncoded
    @POST("api_v1/ticket_transaction_detail.php")
    suspend fun getTransactionDetails(
        @Field("transaction_id") transactionId: Int
    ): Response<TransactionDetailResponse>


    @FormUrlEncoded
    @POST("api_v1/update_privacy.php")
    suspend fun updatePrivacy(
        @Field("is_private") isPrivate: Int // 1 for true, 0 for false
    ): Response<SettingsActionResponse>


    @POST("api_v1/users_deactivate_account.php")
    suspend fun deleteAccount(
    ): Response<SettingsActionResponse>


    @POST("api_v1/logout.php")
    suspend fun logout(
    ): Response<SettingsActionResponse>


    @FormUrlEncoded
    @POST("api_v1/get_bookmarked_events.php")
    suspend fun getBookmarkedEvents(@Field("page") page: Int): Response<DiscoverApiResponse>
}