package com.tie.vibein.tickets.data.repository

import android.content.Context
import android.net.Uri
import com.tie.vibein.credentials.data.retrofit.RetrofitClient
import com.tie.vibein.tickets.data.models.GenericApiResponse
import com.tie.vibein.utils.FileUtils
import com.tie.vibein.utils.NetworkState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class TicketRepository {
    private val api = RetrofitClient.ticketApiEndPoint

    // --- SELLER ---
    suspend fun verifyUpiPayout(userId: String, vpa: String) =
        api.verifyPayout(userId = userId, methodType = "upi", vpa = vpa)

    suspend fun verifyBankPayout(userId: String, accountHolderName: String, ifsc: String, accountNumber: String) =
        api.verifyPayout(
            userId = userId,
            methodType = "bank",
            accountHolderName = accountHolderName,
            ifsc = ifsc,
            accountNumber = accountNumber
        )

    suspend fun createTicket(
        params: Map<String, RequestBody>,
        ticketFile: MultipartBody.Part?
    ): NetworkState<GenericApiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.createTicket(
                    sellerId = params["seller_id"]!!,
                    eventName = params["eventName"]!!,
                    eventDate = params["eventDate"]!!,
                    eventTime = params["eventTime"]!!,
                    eventVenue = params["eventVenue"]!!,
                    originalPrice = params["originalPrice"]!!,
                    sellingPrice = params["sellingPrice"]!!,
                    source = params["source"]!!,
                    ticketFile = ticketFile
                )

                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    NetworkState.Error("Failed to list ticket: $errorMsg")
                }
            } catch (e: Exception) {
                NetworkState.Error("An error occurred: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }


    // --- BUYER ---
    suspend fun browseTickets() = api.browseTickets()

    suspend fun createOrder(ticketId: Int, buyerId: String, buyerName: String, buyerEmail: String, buyerPhone: String) =
        api.createOrder(ticketId, buyerId, buyerName, buyerEmail, buyerPhone)

    suspend fun revealTicket(transactionId: Int, currentUserId: String) =
        api.revealTicket(transactionId, currentUserId)

    suspend fun completeTransaction(transactionId: Int, currentUserId: String) =
        api.completeTransaction(transactionId, currentUserId)

    suspend fun createDispute(transactionId: Int, currentUserId: String, reason: String) =
        api.createDispute(transactionId, currentUserId, reason)
}