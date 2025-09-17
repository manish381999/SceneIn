package com.scenein.settings.data.repository

import com.scenein.credentials.data.retrofit.RetrofitClient // Your existing client
import com.scenein.discover.data.models.FeedItem
import com.scenein.settings.data.models.TransactionDetailResponse
import retrofit2.Response

class SettingsRepository {

    private val api = RetrofitClient.settingsApiEndPoint // We'll add this to RetrofitClient


    suspend fun getTicketTransactionHistory() = api.getTicketTransactionHistory()

    suspend fun getTransactionDetails(transactionId: String?): Response<TransactionDetailResponse> {
        return api.getTransactionDetails(transactionId)
    }

    suspend fun updatePrivacy( isPrivate: Boolean) =
        api.updatePrivacy(if (isPrivate) 1 else 0)

    suspend fun deleteAccount() =
        api.deleteAccount()

    suspend fun logout() =
        api.logout()

    suspend fun getBookmarkedEvents(page: Int): List<FeedItem> {
        val response = api.getBookmarkedEvents(page)
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!.events.map { FeedItem.Event(it) }
        } else {
            throw Exception("Failed to fetch bookmarked events")
        }
    }
}