package com.tie.vibein.chat.data.retrofit

import com.tie.vibein.chat.data.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApiEndPoint {
    @GET("api_v1/get_conversations.php")
    suspend fun getConversations(@Query("user_id") userId: String): Response<GetConversationsResponse>

    @GET("api_v1/get_chat_history.php")
    suspend fun getChatHistory(
        @Query("current_user_id") currentUserId: String,
        @Query("other_user_id") otherUserId: String
    ): Response<GetChatHistoryResponse>

    @FormUrlEncoded
    @POST("api_v1/send_message.php")
    suspend fun sendMessage(
        @Field("sender_id") senderId: String,
        @Field("receiver_id") receiverId: String,
        @Field("message_type") messageType: String,
        @Field("message_content") messageContent: String
    ): Response<SendMessageResponse>

    @Multipart
    @POST("api_v1/handle_chat_upload.php")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): Response<MediaUploadResponse>
}