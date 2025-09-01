package com.scenein.profile.persentation.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.scenein.profile.data.models.ConnectionProfile
import com.scenein.profile.data.models.ConnectionStatusResponse
import com.scenein.profile.data.models.Event
import com.scenein.profile.data.models.MyProfileData
import com.scenein.profile.data.models.MyTicketsActivityResponse
import com.scenein.profile.data.models.PublicProfileData
import com.scenein.profile.data.repository.ProfileRepository
import com.scenein.utils.NetworkState

import kotlinx.coroutines.launch
import org.json.JSONObject

class ProfileViewModel : ViewModel() {
    private val repository = ProfileRepository()

    private val _myProfileState = MutableLiveData<NetworkState<MyProfileData>>()
    val myProfileState: LiveData<NetworkState<MyProfileData>> get() = _myProfileState

    private val _publicProfileState = MutableLiveData<NetworkState<PublicProfileData>>()
    val publicProfileState: LiveData<NetworkState<PublicProfileData>> get() = _publicProfileState

    private val _eventsState = MutableLiveData<NetworkState<List<Event>>>()
    val eventsState: LiveData<NetworkState<List<Event>>> get() = _eventsState


    private val _connectionStatusState = MutableLiveData<NetworkState<ConnectionStatusResponse>>()
    val connectionStatusState: LiveData<NetworkState<ConnectionStatusResponse>> get() = _connectionStatusState

    // This LiveData is for showing Toasts/messages after an action (connect/disconnect)
    private val _connectionActionState = MutableLiveData<NetworkState<String>>()
    val connectionActionState: LiveData<NetworkState<String>> get() = _connectionActionState

    // --- NEW: LiveData for the "Tickets" tab ---
    private val _myTicketsState = MutableLiveData<NetworkState<MyTicketsActivityResponse>>()
    val myTicketsState: LiveData<NetworkState<MyTicketsActivityResponse>> get() = _myTicketsState

    // --- NEW: LiveData for the "Connections" tab ---
    private val _userConnectionsState = MutableLiveData<NetworkState<List<ConnectionProfile>>>()
    val myConnectionsState: LiveData<NetworkState<List<ConnectionProfile>>> get() = _userConnectionsState

    fun fetchMyProfile() {
        _myProfileState.value = NetworkState.Loading
        viewModelScope.launch {
            _myProfileState.value = try {
                val response = repository.getMyProfile()
                if (response.isSuccessful) { NetworkState.Success(response.body()!!.user) }
                else { NetworkState.Error("Failed to load your profile.") }
            } catch (e: Exception) {
                e.message?.let { NetworkState.Error(it) }
            }
        }
    }

    // --- UPDATED to take page and limit from the UI ---
    fun fetchPublicUserProfile(profileId: String) {
        _publicProfileState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                // No longer passes page/limit, matching the simplified repository/API
                val response = repository.getPublicUserProfile(profileId)
                if (response.isSuccessful && response.body() != null) {
                    _publicProfileState.postValue(NetworkState.Success(response.body()!!.user))
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    _publicProfileState.postValue(NetworkState.Error(errorMsg))
                }
            } catch (e: Exception) {
                _publicProfileState.postValue(NetworkState.Error(e.message ?: "An error occurred."))
            }
        }
    }

    fun fetchMyEvents() {
        _eventsState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getEventsByUser(null)
                if (response.isSuccessful && response.body() != null) {
                    // Extract the 'events' list from the response body
                    val eventsList = response.body()!!.events
                    _eventsState.postValue(NetworkState.Success(eventsList))
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    _eventsState.postValue(NetworkState.Error(errorMsg))
                }
            } catch (e: Exception) {
                _eventsState.postValue(NetworkState.Error(e.message ?: "An error occurred."))
            }
        }
    }

    // --- FUNCTION 2: To fetch events for ANY OTHER user's public profile ---
    fun fetchPublicUserEvents(userId: String) {
        _eventsState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getEventsByUser(userId)
                if (response.isSuccessful && response.body() != null) {
                    // Extract the 'events' list from the response body
                    val eventsList = response.body()!!.events
                    _eventsState.postValue(NetworkState.Success(eventsList))
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    _eventsState.postValue(NetworkState.Error(errorMsg))
                }
            } catch (e: Exception) {
                _eventsState.postValue(NetworkState.Error(e.message ?: "An error occurred."))
            }
        }
    }

    fun checkConnectionStatus( profileId: String) {
        _connectionStatusState.postValue(NetworkState.Loading)
        viewModelScope.launch {
            try {
                val response = repository.checkConnectionStatus( profileId)
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

    fun sendConnectionRequest( receiverId: String) {
        _connectionActionState.postValue(NetworkState.Loading)
        viewModelScope.launch {
            try {
                val response = repository.sendConnectionRequest(receiverId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _connectionActionState.postValue(NetworkState.Success(response.body()!!.message))
                    // IMPORTANT: After sending, re-check the status to update the button
                    checkConnectionStatus(receiverId)
                } else {
                    val message = response.body()?.message ?: "Failed to send request"
                    _connectionActionState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _connectionActionState.postValue(NetworkState.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }

    fun removeConnection( otherUserId: String) {
        _connectionActionState.postValue(NetworkState.Loading)
        viewModelScope.launch {
            try {
                val response = repository.removeConnection( otherUserId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _connectionActionState.postValue(NetworkState.Success(response.body()!!.message))
                    // IMPORTANT: After removing, re-check the status to update the button
                    checkConnectionStatus( otherUserId)
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
                    removeConnection( profileId)
                    return@launch
                }

                // For an "accept" action, we call the respond API.
                val apiResponse = repository.respondToConnectionRequest(connectionId, response)
                if (apiResponse.isSuccessful && apiResponse.body()?.status == "success") {
                    _connectionActionState.postValue(NetworkState.Success(apiResponse.body()!!.message))
                    // After responding, re-check the status to update the UI to "Disconnect"
                    checkConnectionStatus(profileId)
                } else {
                    val message = apiResponse.body()?.message ?: "Failed to respond to request"
                    _connectionActionState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _connectionActionState.postValue(NetworkState.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }

    fun fetchMyTicketsActivity() {
        _myTicketsState.value = NetworkState.Loading
        viewModelScope.launch {
            _myTicketsState.value = try {
                val response = repository.getMyTicketsActivity()
                if (response.isSuccessful) NetworkState.Success(response.body()!!)
                else NetworkState.Error("Failed to load tickets.")
            } catch (e: Exception) {
                e.message?.let { NetworkState.Error(it) }
            }
        }
    }

    // --- FUNCTION 1: To fetch connections for the LOGGED-IN user ---
    fun fetchMyConnections() {
        _userConnectionsState.value = NetworkState.Loading
        viewModelScope.launch {
            _userConnectionsState.value = try {
                // Call repository with null to get the logged-in user's connections
                val response = repository.getMyConnections(null)
                if (response.isSuccessful && response.body() != null) {
                    // Correctly extract the 'connections' list from the response
                    NetworkState.Success(response.body()!!.connections)
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred.")
            }
        }
    }

    // --- THIS IS THE CORRECTED fetchPublicUserConnections FUNCTION ---
    fun fetchPublicUserConnections(userId: String) {
        _userConnectionsState.value = NetworkState.Loading
        viewModelScope.launch {
            _userConnectionsState.value = try {
                // Call repository with a specific user ID
                val response = repository.getMyConnections(userId)
                if (response.isSuccessful && response.body() != null) {
                    // Correctly extract the 'connections' list from the response
                    NetworkState.Success(response.body()!!.connections)
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An error occurred.")
            }
        }
    }


}
