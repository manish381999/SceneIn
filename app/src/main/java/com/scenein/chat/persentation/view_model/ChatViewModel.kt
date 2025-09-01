package com.scenein.chat.persentation.view_model

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenein.chat.data.model.Conversation
import com.scenein.chat.data.model.Message
import com.scenein.chat.data.model.MessageStatus
import com.scenein.chat.data.model.SendMessageResponse
import com.scenein.chat.data.model.createImageUrlJson
import com.scenein.chat.data.repository.ChatRepository
import com.scenein.utils.FileUtils
import com.scenein.utils.NetworkState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch


enum class ChatFilter {
    ALL, REQUESTS, UNREAD
}

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()

    private var allFetchedConversations = listOf<Conversation>()
    private val _conversationsState = MutableLiveData<NetworkState<List<Conversation>>>()
    val conversationsState: LiveData<NetworkState<List<Conversation>>> = _conversationsState

    private val _currentFilter = MutableLiveData(ChatFilter.ALL)
    val currentFilter: LiveData<ChatFilter> = _currentFilter

    private val _chatHistoryState = MutableLiveData<NetworkState<List<Message>>>()
    val chatHistoryState: LiveData<NetworkState<List<Message>>> = _chatHistoryState


    private val _sendMessageState = MutableLiveData<NetworkState<SendMessageResponse>>()
    val sendMessageState: LiveData<NetworkState<SendMessageResponse>> = _sendMessageState

    private val _failedMessageTempId = MutableLiveData<String?>()
    val failedMessageTempId: LiveData<String?> = _failedMessageTempId

    private val _messageStatusUpdate = MutableLiveData<Pair<List<String>, MessageStatus>?>()
    val messageStatusUpdate: LiveData<Pair<List<String>, MessageStatus>?> = _messageStatusUpdate

    fun fetchConversations() {
        _conversationsState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getConversations()
                if (response.isSuccessful && response.body()?.status == "success") {
                    allFetchedConversations = response.body()!!.conversations
                    applyFilter(_currentFilter.value)
                } else {
                    _conversationsState.value = NetworkState.Error("Failed to load conversations")
                }
            } catch (e: Exception) {
                _conversationsState.value = NetworkState.Error(e.message ?: "An error occurred")
            }
        }
    }
    fun setFilter(filter: ChatFilter) {
        if (_currentFilter.value == filter) return
        _currentFilter.value = filter
        applyFilter(filter)
    }
    private fun applyFilter(filter: ChatFilter?) {
        val filteredList = when (filter) {
            ChatFilter.REQUESTS -> allFetchedConversations.filter { it.connectionStatus != "accepted" }
            ChatFilter.UNREAD -> allFetchedConversations.filter { it.connectionStatus == "accepted" && it.unreadCount > 0 }
            else -> allFetchedConversations.filter { it.connectionStatus == "accepted" }
        }
        _conversationsState.value = NetworkState.Success(filteredList)
    }
    fun resetFilterToDefault() {
        if (_currentFilter.value != ChatFilter.ALL) {
            _currentFilter.value = ChatFilter.ALL
            applyFilter(ChatFilter.ALL)
        }
    }

    fun fetchChatHistory(currentUserId: String, chatPartnerId: String) {
        _chatHistoryState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getChatHistory(chatPartnerId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val messages = response.body()!!.messages
                    messages.forEach { msg ->
                        if (msg.senderId == currentUserId) {
                            msg.status = when {
                                msg.isRead -> MessageStatus.READ
                                msg.isDelivered -> MessageStatus.DELIVERED
                                else -> MessageStatus.SENT
                            }
                        }
                    }
                    _chatHistoryState.value = NetworkState.Success(messages)
                } else {
                    _chatHistoryState.value = NetworkState.Error("Failed to load chat history")
                }
            } catch (e: Exception) {
                _chatHistoryState.value = NetworkState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun sendMessage(receiverId: String, messageType: String, content: String, tempId: String) {
        viewModelScope.launch {
            try {
                val response = repository.sendMessage(receiverId, messageType, content)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val serverMessage = response.body()!!.sentMessage
                    if (serverMessage != null) {
                        val uiUpdateMessage = serverMessage.copy(tempId = tempId, status = MessageStatus.SENT)
                        val updatedResponse = response.body()!!.copy(sentMessage = uiUpdateMessage)
                        _sendMessageState.value = NetworkState.Success(updatedResponse)
                    } else {
                        throw Exception("Server sent 'success' but no message object was returned.")
                    }
                } else {
                    throw Exception(response.body()?.message ?: "Failed to send message")
                }
            } catch (e: Exception) {
                // ADD THIS LOG TO CATCH ANY FUTURE ERRORS
                Log.e("ChatViewModel", "sendMessage failed", e)
                _sendMessageState.value = NetworkState.Error(e.message ?: "An error occurred")
                _failedMessageTempId.value = tempId
            }
        }
    }

    // ======================================================================
    // == NEW: Function to handle multiple file uploads in parallel ==
    // ======================================================================
    fun uploadMultipleFilesAndSend(
        context: Context,
        receiverId: String,
        mediaUris: List<Uri>,
        tempId: String
    ) {
        viewModelScope.launch {
            try {
                // Launch all uploads in parallel using async
                val uploadJobs = mediaUris.map { uri ->
                    async {
                        val part = FileUtils.getMultipartBodyPartFromUri(context, uri, "uploaded_file")
                            ?: throw Exception("Failed to process URI: $uri")

                        val response = repository.uploadMedia(part)

                        // --- MODIFIED LOGIC ---
                        val responseBody = response.body()
                        val uploadedUrls = responseBody?.fileUrls // Get the new list property

                        if (response.isSuccessful && responseBody?.status == "success" && !uploadedUrls.isNullOrEmpty()) {
                            // Success! Return the first (and only) URL from the list.
                            uploadedUrls.first()
                        } else {
                            val errorBody = response.errorBody()?.string() ?: responseBody?.message ?: "Unknown upload error"
                            throw Exception("Upload failed for $uri: $errorBody")
                        }
                    }
                }

                // Wait for all parallel uploads to complete
                val uploadedUrls = uploadJobs.awaitAll()

                // If we got here, all uploads were successful
                val contentJson = createImageUrlJson(uploadedUrls)
                sendMessage(receiverId, "image", contentJson, tempId)

            } catch (e: Exception) {
                // The exception will now have a more detailed message from the server
                _sendMessageState.value = NetworkState.Error(e.message ?: "One or more uploads failed.")
                _failedMessageTempId.value = tempId
            }
        }
    }


    // --- NEW & CORRECTED: This function simply relays the update info to the LiveData ---
    fun triggerStatusUpdate(messageIds: List<String>, newStatus: MessageStatus) {
        _messageStatusUpdate.value = Pair(messageIds, newStatus)
    }

    fun onStatusUpdateHandled() {
        _messageStatusUpdate.value = null
    }

    fun onFailedMessageHandled() {
        _failedMessageTempId.value = null
    }

    fun markMessageAsRead(messageId: String) {
        // No need to observe the result, as the update will come back via FCM.
        viewModelScope.launch {
            try {
                repository.markMessagesAsRead(listOf(messageId))
            } catch (e: Exception) {
                // Log the error silently, no need to show a UI error for this.
                Log.e("ChatViewModel", "Failed to mark message as read: $messageId", e)
            }
        }
    }
}