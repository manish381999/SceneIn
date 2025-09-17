package com.scenein.tickets.data.respository


import com.scenein.credentials.data.retrofit.RetrofitClient
import com.scenein.tickets.data.models.GenericApiResponse
import com.scenein.tickets.data.models.ParseTextResponse
import com.scenein.utils.NetworkState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

class TicketRepository {
    private val api = RetrofitClient.ticketApiEndPoint

    // --- SELLER ---
    suspend fun verifyUpiPayout( vpa: String) =
        api.verifyPayout( methodType = "upi", vpa = vpa)

    suspend fun verifyBankPayout( accountHolderName: String, ifsc: String, accountNumber: String) =
        api.verifyPayout(
            methodType = "bank",
            accountHolderName = accountHolderName,
            ifsc = ifsc,
            accountNumber = accountNumber
        )

    suspend fun parseOcrText(rawText: String): Response<ParseTextResponse> {
        val requestBody = mapOf("raw_text" to rawText)
        return api.parseOcrText(requestBody)
    }

    suspend fun createTicket(
        params: Map<String, RequestBody>,
        ticketFile: MultipartBody.Part?
    ): NetworkState<GenericApiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.createTicket(
                    eventName = params["eventName"]!!,
                    eventDate = params["eventDate"]!!,
                    eventTime = params["eventTime"]!!,
                    eventVenue = params["eventVenue"]!!,
                    eventCity = params["eventCity"]!!,
                    numberOfTickets = params["numberOfTickets"]!!,
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

    suspend fun updateTicket(ticketId: String, newPrice: String): Response<GenericApiResponse> {
        return api.updateTicket(ticketId, newPrice)
    }

    suspend fun delistTicket(ticketId: String): Response<GenericApiResponse> {
        return api.delistTicket(ticketId)
    }

    suspend fun relistTicket(transactionId: Int, newSellingPrice: String): Response<GenericApiResponse> {
        return api.relistTicket(
            transactionId = transactionId,
            newSellingPrice = newSellingPrice
        )
    }


    // --- BUYER ---
    suspend fun browseTickets() = api.browseTickets()

    suspend fun createOrder(ticketId: String) =
        api.createOrder(ticketId)

    suspend fun getMyActivity() = api.getMyActivity()

    suspend fun revealTicket(transactionId: Int) =
        api.revealTicket(transactionId)

    suspend fun completeTransaction(transactionId: Int) =
        api.completeTransaction(transactionId)

    suspend fun createDispute(transactionId: Int, reason: String) =
        api.createDispute(transactionId, reason)


}