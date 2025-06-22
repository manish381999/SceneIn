package com.tie.vibein.chat.data.repository

import com.tie.vibein.chat.data.models.MarkAsDeliveredRequest
import com.tie.vibein.credentials.data.retrofit.RetrofitClient // Or your specific client
import okhttp3.MultipartBody

class ChatRepository {
    private val api = RetrofitClient.chatApiEndPoint

    suspend fun getConversations(userId: String) = api.getConversations(userId)
    suspend fun getChatHistory(currentUserId: String, otherUserId: String) = api.getChatHistory(currentUserId, otherUserId)
    suspend fun sendMessage(senderId: String, receiverId: String, type: String, content: String) = api.sendMessage(senderId, receiverId, type, content)
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