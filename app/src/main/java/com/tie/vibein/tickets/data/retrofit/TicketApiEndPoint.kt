package com.tie.vibein.tickets.data.retrofit

import com.tie.vibein.tickets.data.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface TicketApiEndPoint {

    // == SELLER FLOW ==

    @FormUrlEncoded
    @POST("api_v1/payouts_verify.php")
    suspend fun verifyPayout(
        @Field("user_id") userId: String,
        @Field("method_type") methodType: String,
        // For complex objects, we send the individual fields
        @Field("vpa") vpa: String? = null,
        @Field("account_holder_name") accountHolderName: String? = null,
        @Field("ifsc") ifsc: String? = null,
        @Field("account_number") accountNumber: String? = null
    ): Response<PayoutVerificationResponse>

    @POST("api_v1/tickets_parse_text.php")
    suspend fun parseOcrText(@Body body: Map<String, String>): Response<ParseTextResponse>

    @Multipart
    @POST("api_v1/tickets_create.php")
    suspend fun createTicket(
        @Part("seller_id") sellerId: RequestBody,
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
    @POST("api_v1/tickets_update.php")
    suspend fun updateTicket(
        @Field("ticket_id") ticketId: Int,
        @Field("seller_id") sellerId: String,
        @Field("selling_price") SellingPrice: String
    ): Response<GenericApiResponse>

    @FormUrlEncoded
    @POST("api_v1/tickets_delist.php")
    suspend fun delistTicket(
        @Field("ticket_id") ticketId: Int,
        @Field("seller_id") sellerId: String
    ): Response<GenericApiResponse>

    @FormUrlEncoded
    @POST("api_v1/tickets_relist.php")
    suspend fun relistTicket(
        @Field("transaction_id") transactionId: Int,
        @Field("new_seller_id") newSellerId: String,
        @Field("new_selling_price") newSellingPrice: String
    ): Response<GenericApiResponse>



    // == BUYER FLOW ==

    @GET("api_v1/tickets_browse.php")
    suspend fun browseTickets(): Response<BrowseTicketsResponse>

    @FormUrlEncoded
    @POST("api_v1/orders_create.php")
    suspend fun createOrder(
        @Field("ticket_id") ticketId: Int,
        @Field("buyer_id") buyerId: String,
        @Field("buyer_name") buyerName: String,
        @Field("buyer_email") buyerEmail: String,
        @Field("buyer_phone") buyerPhone: String
    ): Response<CreateOrderResponse>

    @FormUrlEncoded
    @POST("api_v1/tickets_my_activity.php") // Using POST with form-data
    suspend fun getMyActivity(@Field("user_id") userId: String): Response<MyActivityResponse>

    @FormUrlEncoded
    @POST("api_v1/tickets_reveal.php")
    suspend fun revealTicket(
        @Field("transaction_id") transactionId: Int,
        @Field("current_user_id") currentUserId: String
    ): Response<GenericApiResponse>

    @FormUrlEncoded
    @POST("api_v1/transactions_complete.php")
    suspend fun completeTransaction(
        @Field("transaction_id") transactionId: Int,
        @Field("current_user_id") currentUserId: String
    ): Response<GenericApiResponse>

    @FormUrlEncoded
    @POST("api_v1/disputes_create.php")
    suspend fun createDispute(
        @Field("transaction_id") transactionId: Int,
        @Field("current_user_id") currentUserId: String,
        @Field("reason") reason: String
    ): Response<GenericApiResponse>


}