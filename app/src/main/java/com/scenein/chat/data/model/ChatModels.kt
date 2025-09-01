package com.scenein.chat.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.Serializable
import java.util.UUID

// The statuses your UI will use
enum class MessageStatus {
    SENDING,
    SENT,       // Single Tick
    DELIVERED,  // Double Tick
    READ,       // Blue Double Tick
    FAILED
}

// Main Message data class
data class Message(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("message_type") var messageType: String,
    @SerializedName("message_content") var messageContent: String,
    @SerializedName("timestamp") var timestamp: String,

    // --- NEW: These fields will now be correctly parsed from the server ---
    @SerializedName("is_delivered") val isDelivered: Boolean,
    @SerializedName("is_read") val isRead: Boolean,

    // --- UI-only properties ---
    var status: MessageStatus = MessageStatus.SENT,
    var tempId: String? = UUID.randomUUID().toString()

) : Serializable {
    fun getImageUrls(): List<String> {
        if (messageType != "image" || messageContent.isBlank()) {
            return emptyList()
        }
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(messageContent, type)
        } catch (e: Exception) {
            listOf(messageContent)
        }
    }
}

// Helper to create the JSON for sending image URLs
fun createImageUrlJson(urls: List<String>): String {
    return Gson().toJson(urls)
}

// Wrapper classes for API responses
data class GetConversationsResponse(
    val status: String,
    val conversations: List<Conversation>
)
data class GetChatHistoryResponse(
    val status: String,
    val messages: List<Message>
)
data class MediaUploadResponse(
    val status: String,
    val message: String,
    @SerializedName("file_urls")
    val fileUrls: List<String>?
)
data class SendMessageResponse(
    val status: String,
    val message: String,
    @SerializedName("sent_message") val sentMessage: Message?
)
data class Conversation(
    @SerializedName("other_user_id") val otherUserId: String,
    @SerializedName("other_user_name") val name: String,
    @SerializedName("other_user_profile_pic") val profilePic: String,
    @SerializedName("last_message") val lastMessage: String,
    @SerializedName("last_message_timestamp") val timestamp: String,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("connection_status") val connectionStatus: String
)

// Data class for the 'mark_delivered' request body
data class MarkAsDeliveredRequest(
    @SerializedName("message_ids")
    val messageIds: List<String>
)