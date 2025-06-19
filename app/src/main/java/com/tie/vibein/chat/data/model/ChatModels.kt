package com.tie.vibein.chat.data.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.UUID

data class Message(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("message_type") var messageType: String,
    @SerializedName("message_content") var messageContent: String,
    @SerializedName("timestamp") var timestamp: String,

    // UI-only properties with default values.
    // They MUST be `var` to be updatable.
    var status: MessageStatus = MessageStatus.SENT,
    var tempId: String = UUID.randomUUID().toString()

) : Serializable

enum class MessageStatus {
    SENDING, SENT, FAILED
}
// Data class for the main conversation list in ChatFragment.
data class Conversation(
    @SerializedName("other_user_id") val otherUserId: String,
    @SerializedName("other_user_name") val name: String,
    @SerializedName("other_user_profile_pic") val profilePic: String,
    @SerializedName("last_message") val lastMessage: String,
    @SerializedName("last_message_timestamp") val timestamp: String,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("connection_status") val connectionStatus: String
)

// Wrapper classes for API responses.
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
    @SerializedName("file_url") val fileUrl: String?
)

data class SendMessageResponse(
    val status: String,
    val message: String,
    @SerializedName("sent_message") val sentMessage: Message?
)