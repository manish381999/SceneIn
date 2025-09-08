package com.scenein.chat.data.repository

import com.scenein.chat.data.model.MarkAsDeliveredRequest
import com.scenein.credentials.data.retrofit.RetrofitClient
import okhttp3.MultipartBody

class ChatRepository {
    private val api = RetrofitClient.chatApiEndPoint

    suspend fun getConversations() = api.getConversations()
    suspend fun getChatHistory(chatPartnerId: String) = api.getChatHistory(chatPartnerId)
    suspend fun sendMessage(receiverId: String, type: String, content: String) = api.sendMessage(receiverId, type, content)
    suspend fun uploadMedia(file: MultipartBody.Part) = api.uploadMedia(file)
    suspend fun markMessagesAsDelivered(messageIds: List<String>) {
        val request = MarkAsDeliveredRequest(messageIds)
        api.markMessagesAsDelivered(request)
    }

    suspend fun markMessagesAsRead(messageIds: List<String>) {
        val request = MarkAsDeliveredRequest(messageIds)
        api.markMessagesAsRead(request)
    }


}