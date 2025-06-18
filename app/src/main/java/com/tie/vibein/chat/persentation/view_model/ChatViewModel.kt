package com.tie.vibein.chat.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tie.vibein.chat.data.models.*
import com.tie.vibein.chat.data.repository.ChatRepository
import com.tie.vibein.utils.FileUtils
import com.tie.vibein.utils.NetworkState
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

// This enum for filters is correct.
enum class ChatFilter {
    ALL, REQUESTS, UNREAD
}

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()

    // --- Private properties to hold the original, complete lists from the API ---
    private var allFetchedConversations = listOf<Conversation>()

    // --- LiveData for the UI to observe ---
    private val _conversationsState = MutableLiveData<NetworkState<List<Conversation>>>()
    val conversationsState: LiveData<NetworkState<List<Conversation>>> = _conversationsState

    private val _currentFilter = MutableLiveData(ChatFilter.ALL)
    val currentFilter: LiveData<ChatFilter> = _currentFilter

    private val _chatHistoryState = MutableLiveData<NetworkState<List<Message>>>()
    val chatHistoryState: LiveData<NetworkState<List<Message>>> = _chatHistoryState

    // LiveData to report the SUCCESSFUL sending of a message
    private val _sendMessageState = MutableLiveData<NetworkState<SendMessageResponse>>()
    val sendMessageState: LiveData<NetworkState<SendMessageResponse>> = _sendMessageState

    private val _uploadState = MutableLiveData<NetworkState<String>>()
    val uploadState: LiveData<NetworkState<String>> = _uploadState

    // **THE FIX**: A dedicated LiveData to report only the tempId of a FAILED message.
    private val _failedMessageTempId = MutableLiveData<String?>()
    val failedMessageTempId: LiveData<String?> = _failedMessageTempId

    // fetchConversations and its filter logic are correct and do not need changes.
    fun fetchConversations(userId: String) {
        _conversationsState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getConversations(userId)
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

    // fetchChatHistory is correct and does not need changes.
    fun fetchChatHistory(currentUserId: String, otherUserId: String) {
        _chatHistoryState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getChatHistory(currentUserId, otherUserId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _chatHistoryState.value = NetworkState.Success(response.body()!!.messages)
                } else {
                    _chatHistoryState.value = NetworkState.Error("Failed to load chat history")
                }
            } catch (e: Exception) {
                _chatHistoryState.value = NetworkState.Error(e.message ?: "An error occurred")
            }
        }
    }

    // This function now safely handles both success and failure cases without changing NetworkState.
    fun sendMessage(senderId: String, receiverId: String, messageType: String, content: String, tempId: String) {
        viewModelScope.launch {
            try {
                val response = repository.sendMessage(senderId, receiverId, messageType, content)
                if (response.isSuccessful && response.body()?.status == "success") {
                    // Success: Get the message from the server's response.
                    val serverMessage = response.body()!!.sentMessage

                    if (serverMessage != null) {
                        // Manually create a new message object for the UI update,
                        // ensuring the tempId is preserved.
                        val uiUpdateMessage = Message(
                            messageId = serverMessage.messageId,
                            senderId = serverMessage.senderId,
                            receiverId = serverMessage.receiverId,
                            messageType = serverMessage.messageType,
                            messageContent = serverMessage.messageContent,
                            timestamp = serverMessage.timestamp
                        ).apply {
                            this.tempId = tempId // IMPORTANT: Carry over the original tempId
                            this.status = MessageStatus.SENT
                        }

                        // Create a new response object with our manually updated message.
                        val updatedResponse = response.body()!!.copy(sentMessage = uiUpdateMessage)
                        _sendMessageState.value = NetworkState.Success(updatedResponse)

                    } else {
                        // Handle case where server sent success but no message object
                        _sendMessageState.value = NetworkState.Error("Server response was empty.")
                        _failedMessageTempId.value = tempId
                    }
                } else {
                    _sendMessageState.value = NetworkState.Error(response.body()?.message ?: "Failed to send")
                    _failedMessageTempId.value = tempId
                }
            } catch (e: Exception) {
                _sendMessageState.value = NetworkState.Error(e.message ?: "An error occurred")
                _failedMessageTempId.value = tempId
            }
        }
    }

    // --- THIS IS THE CORRECTED uploadMediaAndSendMessage FUNCTION ---
    fun uploadMediaAndSendMessage(context: Context, senderId: String, receiverId: String, mediaUri: Uri, messageType: String, tempId: String) {
        _uploadState.value = NetworkState.Loading
        viewModelScope.launch {
            val mediaFile = FileUtils.getFileFromUri(context.applicationContext, mediaUri)
            if (mediaFile == null) {
                // Failure Case: Update the error states correctly.
                _uploadState.value = NetworkState.Error("Failed to access selected file")
                _sendMessageState.value = NetworkState.Error("File access error")
                _failedMessageTempId.value = tempId
                return@launch
            }

            try {
                val requestFile = mediaFile.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("uploaded_file", mediaFile.name, requestFile)
                val uploadResponse = repository.uploadMedia(body)

                if (uploadResponse.isSuccessful && uploadResponse.body()?.status == "success") {
                    val fileUrl = uploadResponse.body()!!.fileUrl
                    if (fileUrl.isNullOrEmpty()) throw Exception("Server returned an empty URL.")

                    _uploadState.value = NetworkState.Success("Upload complete.")
                    sendMessage(senderId, receiverId, messageType, fileUrl, tempId)

                } else {
                    throw Exception(uploadResponse.body()?.message ?: "Media upload failed on server.")
                }
            } catch (e: Exception) {
                _uploadState.value = NetworkState.Error(e.message ?: "An error occurred during upload")
                _sendMessageState.value = NetworkState.Error(e.message ?: "Upload failed")
                _failedMessageTempId.value = tempId // Report failure using tempId
            } finally {
                mediaFile.delete()
            }
        }
    }

    // Public function for the UI to call after it has handled the failure state.
    fun onFailedMessageHandled() {
        _failedMessageTempId.value = null
    }
}