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


    @GET("chat/get_conversations")
    suspend fun getConversations(): Response<GetConversationsResponse>

    @GET("chat/get_history/{id}")
    suspend fun getChatHistory(
        @Path("id") chatPartnerId: String
    ): Response<GetChatHistoryResponse>


    @FormUrlEncoded
    @POST("chat/send_chat")
    suspend fun sendMessage(
        @Field("receiver_id") receiverId: String,
        @Field("message_type") messageType: String,
        @Field("message_content") messageContent: String
    ): Response<SendMessageResponse>

    @Multipart
    @POST("chat/upload_media")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): Response<MediaUploadResponse>

    @POST("chat/mark_delivered")
    suspend fun markMessagesAsDelivered(@Body body: MarkAsDeliveredRequest): Response<Unit>

    @POST("chat/mark_read")
    suspend fun markMessagesAsRead(@Body body: MarkAsDeliveredRequest): Response<Unit>



}