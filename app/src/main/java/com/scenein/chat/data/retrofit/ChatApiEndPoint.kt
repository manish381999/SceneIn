package com.scenein.chat.data.retrofit

import com.scenein.chat.data.model.GetChatHistoryResponse
import com.scenein.chat.data.model.GetConversationsResponse
import com.scenein.chat.data.model.MarkAsDeliveredRequest
import com.scenein.chat.data.model.MediaUploadResponse
import com.scenein.chat.data.model.SendMessageResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApiEndPoint {


    @GET("api_v1/get_conversations.php")
    suspend fun getConversations(): Response<GetConversationsResponse>

    @GET("api_v1/get_chat_history.php")
    suspend fun getChatHistory(
        @Query("chat_partner_id") chatPartnerId: String
    ): Response<GetChatHistoryResponse>


    @FormUrlEncoded
    @POST("api_v1/send_message.php")
    suspend fun sendMessage(
        @Field("receiver_id") receiverId: String,
        @Field("message_type") messageType: String,
        @Field("message_content") messageContent: String
    ): Response<SendMessageResponse>

    @Multipart
    @POST("api_v1/handle_chat_upload.php")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): Response<MediaUploadResponse>

    @POST("api_v1/mark_delivered.php")
    suspend fun markMessagesAsDelivered(@Body body: MarkAsDeliveredRequest): Response<Unit>

    @POST("api_v1/mark_read.php")
    suspend fun markMessagesAsRead(@Body body: MarkAsDeliveredRequest): Response<Unit>
}