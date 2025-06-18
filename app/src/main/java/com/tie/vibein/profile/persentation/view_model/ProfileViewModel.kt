package com.tie.vibein.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tie.vibein.profile.data.models.ConnectionStatusResponse
import com.tie.vibein.profile.data.models.Event
import com.tie.vibein.profile.data.repository.ProfileRepository
import com.tie.vibein.utils.NetworkState
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = ProfileRepository()

    private val _eventsState = MutableLiveData<NetworkState<List<Event>>>()
    val eventsState: LiveData<NetworkState<List<Event>>> get() = _eventsState

    private val _connectionStatusState = MutableLiveData<NetworkState<ConnectionStatusResponse>>()
    val connectionStatusState: LiveData<NetworkState<ConnectionStatusResponse>> get() = _connectionStatusState

    // This LiveData is for showing Toasts/messages after an action (connect/disconnect)
    private val _connectionActionState = MutableLiveData<NetworkState<String>>()
    val connectionActionState: LiveData<NetworkState<String>> get() = _connectionActionState

    fun fetchEventsByUser(userId: String) {
        _eventsState.postValue(NetworkState.Loading)

        viewModelScope.launch {
            try {
                val response = repository.getEventsByUser(userId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val events = response.body()?.events ?: emptyList()
                    _eventsState.postValue(NetworkState.Success(events))
                } else {
                    val message = response.body()?.message ?: "Unknown error"
                    _eventsState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _eventsState.postValue(NetworkState.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }

    fun checkConnectionStatus(viewerId: String, profileId: String) {
        _connectionStatusState.postValue(NetworkState.Loading)
        viewModelScope.launch {
            try {
                val response = repository.checkConnectionStatus(viewerId, profileId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _connectionStatusState.postValue(NetworkState.Success(response.body()!!))
                } else {
                    _connectionStatusState.postValue(NetworkState.Error("Failed to check status"))
                }
            } catch (e: Exception) {
                _connectionStatusState.postValue(NetworkState.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }

    fun sendConnectionRequest(senderId: String, receiverId: String) {
        _connectionActionState.postValue(NetworkState.Loading)
        viewModelScope.launch {
            try {
                val response = repository.sendConnectionRequest(senderId, receiverId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _connectionActionState.postValue(NetworkState.Success(response.body()!!.message))
                    // IMPORTANT: After sending, re-check the status to update the button
                    checkConnectionStatus(senderId, receiverId)
                } else {
                    val message = response.body()?.message ?: "Failed to send request"
                    _connectionActionState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _connectionActionState.postValue(NetworkState.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }

    fun removeConnection(currentUserId: String, userToDisconnectId: String) {
        _connectionActionState.postValue(NetworkState.Loading)
        viewModelScope.launch {
            try {
                val response = repository.removeConnection(currentUserId, userToDisconnectId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _connectionActionState.postValue(NetworkState.Success(response.body()!!.message))
                    // IMPORTANT: After removing, re-check the status to update the button
                    checkConnectionStatus(currentUserId, userToDisconnectId)
                } else {
                    val message = response.body()?.message ?: "Failed to remove connection"
                    _connectionActionState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _connectionActionState.postValue(NetworkState.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }
    fun respondToConnectionRequest(connectionId: String, response: String, viewerId: String, profileId: String) {
        _connectionActionState.postValue(NetworkState.Loading)
        viewModelScope.launch {
            try {
                // For a "decline" action, we just remove the connection record entirely.
                if (response == "declined") {
                    removeConnection(viewerId, profileId)
                    return@launch
                }

                // For an "accept" action, we call the respond API.
                val apiResponse = repository.respondToConnectionRequest(connectionId, response)
                if (apiResponse.isSuccessful && apiResponse.body()?.status == "success") {
                    _connectionActionState.postValue(NetworkState.Success(apiResponse.body()!!.message))
                    // After responding, re-check the status to update the UI to "Disconnect"
                    checkConnectionStatus(viewerId, profileId)
                } else {
                    val message = apiResponse.body()?.message ?: "Failed to respond to request"
                    _connectionActionState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _connectionActionState.postValue(NetworkState.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }
}
