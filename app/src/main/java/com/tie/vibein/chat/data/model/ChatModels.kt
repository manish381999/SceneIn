package com.tie.vibein.chat.data.models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.Serializable
import java.util.UUID

// ======================================================================
// == UPDATED Message DATA CLASS ==
// ======================================================================

data class Message(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("message_type") var messageType: String, // "text" or "image"
    @SerializedName("message_content") var messageContent: String, // For text, this is the message. For images, this is a JSON array of URLs.
    @SerializedName("timestamp") var timestamp: String,

    // UI-only properties
    var status: MessageStatus = MessageStatus.SENT,
    var tempId: String = UUID.randomUUID().toString()

) : Serializable {

    // --- NEW: Helper function to get image URLs from the JSON content ---
    fun getImageUrls(): List<String> {
        // Only try to parse if the message type is "image"
        if (messageType != "image" || messageContent.isBlank()) {
            return emptyList()
        }
        return try {
            // Use Gson to parse the JSON array string into a List<String>
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(messageContent, type)
        } catch (e: Exception) {
            // For backward compatibility: if parsing fails, it might be an old, single URL.
            // Wrap it in a list and return it.
            listOf(messageContent)
        }
    }
}

// --- NEW: Top-level helper function to create the JSON string for sending ---
fun createImageUrlJson(urls: List<String>): String {
    return Gson().toJson(urls)
}


// ======================================================================
// == NO CHANGES NEEDED BELOW THIS LINE ==
// ======================================================================

enum class MessageStatus {
    SENDING, SENT, FAILED
}

// Data class for the main conversation list in ChatFragment.
// No changes needed here. The server should summarize multi-image messages into the `lastMessage` string (e.g., "📷 3 Photos").
data class Conversation(
    @SerializedName("other_user_id") val otherUserId: String,
    @SerializedName("other_user_name") val name: String,
    @SerializedName("other_user_profile_pic") val profilePic: String,
    @SerializedName("last_message") val lastMessage: String,
    @SerializedName("last_message_timestamp") val timestamp: String,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("connection_status") val connectionStatus: String
)

// Wrapper classes for API responses. No changes needed.
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