package com.tie.vibein.chat.data.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.UUID


// FINAL, STABLE VERSION of the Message data class
data class Message(
    // These properties are created directly by Gson from your server's JSON response
    @SerializedName("message_id") val messageId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("message_type") val messageType: String,
    @SerializedName("message_content") val messageContent: String,
    @SerializedName("timestamp") val timestamp: String
) : Serializable {
    // UI-only properties are declared inside the class body with default values.
    // This ensures they are NEVER null, fixing the crash in your adapter.
    var tempId: String = UUID.randomUUID().toString()
    var status: MessageStatus = MessageStatus.SENT
}

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
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