package com.scenein.tickets.data.retrofit

import com.scenein.tickets.data.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface TicketApiEndPoint {

    // == SELLER FLOW ==

    @FormUrlEncoded
    @POST("users/verify_payout_method")
    suspend fun verifyPayout(
        @Field("method_type") methodType: String,
        // For complex objects, we send the individual fields
        @Field("vpa") vpa: String? = null,
        @Field("account_holder_name") accountHolderName: String? = null,
        @Field("ifsc") ifsc: String? = null,
        @Field("account_number") accountNumber: String? = null
    ): Response<PayoutVerificationResponse>

    @POST("tickets/parse_text")
    suspend fun parseOcrText(@Body body: Map<String, String>): Response<ParseTextResponse>

    @Multipart
    @POST("tickets/create_ticket")
    suspend fun createTicket(
        @Part("eventName") eventName: RequestBody,
        @Part("eventDate") eventDate: RequestBody,
        @Part("eventTime") eventTime: RequestBody,
        @Part("eventVenue") eventVenue: RequestBody,
        @Part("eventCity") eventCity: RequestBody,
        @Part("numberOfTickets") numberOfTickets: RequestBody,
        @Part("originalPrice") originalPrice: RequestBody,
        @Part("sellingPrice") sellingPrice: RequestBody,
        @Part("source") source: RequestBody,
        @Part ticketFile: MultipartBody.Part? // Nullable, just like your cover_image
    ): Response<GenericApiResponse>

    @FormUrlEncoded
    @POST("tickets/update_ticket")
    suspend fun updateTicket(
        @Field("ticket_id") ticketId: String,
        @Field("selling_price") SellingPrice: String
    ): Response<GenericApiResponse>

    @FormUrlEncoded
    @POST("tickets/delist_ticket")
    suspend fun delistTicket(
        @Field("ticket_id") ticketId: String
    ): Response<GenericApiResponse>

    @FormUrlEncoded
    @POST("tickets/relist")
    suspend fun relistTicket(
        @Field("transaction_id") transactionId: Int,
        @Field("new_selling_price") newSellingPrice: String
    ): Response<GenericApiResponse>



    // == BUYER FLOW ==

    @GET("tickets/browse_ticket")
    suspend fun browseTickets(): Response<BrowseTicketsResponse>

    @FormUrlEncoded
    @POST("tickets/create_ticket_payment_order")
    suspend fun createOrder(
        @Field("ticket_id") ticketId: String
    ): Response<CreateOrderResponse>


    @GET("tickets/my_activity")
    suspend fun getMyActivity(): Response<MyActivityResponse>


    @FormUrlEncoded
    @POST("tickets/reveal_ticket")
    suspend fun revealTicket(
        @Field("transaction_id") transactionId: Int,
    ): Response<GenericApiResponse>

    @FormUrlEncoded
    @POST("tickets/complete_ticket_purchase")
    suspend fun completeTransaction(
        @Field("transaction_id") transactionId: Int,
    ): Response<GenericApiResponse>

    @FormUrlEncoded
    @POST("tickets/create_ticket_dispute")
    suspend fun createDispute(
        @Field("transaction_id") transactionId: Int,
        @Field("reason") reason: String
    ): Response<GenericApiResponse>


}