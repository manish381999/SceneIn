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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

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

    fun sendMessage(senderId: String, receiverId: String, messageType: String, content: String, tempId: String) {
        viewModelScope.launch {
            try {
                val response = repository.sendMessage(senderId, receiverId, messageType, content)
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
        senderId: String,
        receiverId: String,
        mediaUris: List<Uri>,
        tempId: String
    ) {
        viewModelScope.launch {
            try {
                // Launch all uploads in parallel using async
                val uploadJobs = mediaUris.map { uri ->
                    async {
                        val mediaFile = FileUtils.getFileFromUri(context.applicationContext, uri)
                            ?: throw Exception("Failed to process URI: $uri")
                        try {
                            val mimeType = context.contentResolver.getType(uri) ?: "image/*"
                            val requestFile = mediaFile.asRequestBody(mimeType.toMediaTypeOrNull())
                            val body = MultipartBody.Part.createFormData("uploaded_file", mediaFile.name, requestFile)
                            val response = repository.uploadMedia(body)
                            if (response.isSuccessful && response.body()?.status == "success" && !response.body()!!.fileUrl.isNullOrEmpty()) {
                                response.body()!!.fileUrl!!
                            } else {
                                throw Exception("Upload failed for $uri")
                            }
                        } finally {
                            mediaFile.delete() // Ensure temp file is always deleted
                        }
                    }
                }

                // Wait for all parallel uploads to complete
                val uploadedUrls = uploadJobs.awaitAll()

                // If we got here, all uploads were successful
                val contentJson = createImageUrlJson(uploadedUrls)
                sendMessage(senderId, receiverId, "image", contentJson, tempId)

            } catch (e: Exception) {
                // If any of the uploads fail, the entire operation fails
                _sendMessageState.value = NetworkState.Error(e.message ?: "One or more uploads failed.")
                _failedMessageTempId.value = tempId
            }
        }
    }


    fun onFailedMessageHandled() {
        _failedMessageTempId.value = null
    }
}