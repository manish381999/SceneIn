package com.tie.vibein.discover.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tie.vibein.createEvent.data.models.ApiResponse
import com.tie.vibein.discover.data.model.EventSummary
import com.tie.vibein.discover.data.models.EventDetail
import com.tie.vibein.discover.data.models.GetUsersResponse
import com.tie.vibein.discover.data.repository.DiscoverRepository
import com.tie.vibein.utils.NetworkState
import kotlinx.coroutines.launch
import retrofit2.Response

class DiscoverViewModel : ViewModel() {

    private val repository = DiscoverRepository()

    // LiveData for list of events (EventSummary)
    private val _eventState = MutableLiveData<NetworkState<List<EventSummary>>>()
    val eventState: LiveData<NetworkState<List<EventSummary>>> get() = _eventState

    // LiveData for joining event response
    private val _joinEventState = MutableLiveData<NetworkState<ApiResponse>?>()
    val joinEventState: LiveData<NetworkState<ApiResponse>?> get() = _joinEventState

    // LiveData for unjoining event response
    private val _unjoinEventState = MutableLiveData<NetworkState<ApiResponse>?>()
    val unjoinEventState: LiveData<NetworkState<ApiResponse>?> get() = _unjoinEventState

    // LiveData for user details
    private val _userDetailsState = MutableLiveData<NetworkState<List<GetUsersResponse.User>>>()
    val userDetailsState: LiveData<NetworkState<List<GetUsersResponse.User>>> get() = _userDetailsState

    // LiveData for single event detail (EventDetail)
    private val _eventDetailsState = MutableLiveData<NetworkState<EventDetail?>>()
    val eventDetailsState: LiveData<NetworkState<EventDetail?>> get() = _eventDetailsState

    // Fetch events by city (list of EventSummary)
    fun fetchCityEvents(userId: String, city: String, currentDate: String) {
        _eventState.value = NetworkState.Loading

        viewModelScope.launch {
            try {
                // Explicitly define response type for better inference
                val response: Response<com.tie.vibein.discover.data.model.GetEventsResponse> =
                    repository.getEventsByCity(userId, city, currentDate)

                if (response.isSuccessful && response.body()?.status == "success") {
                    val events: List<EventSummary> = response.body()?.events ?: emptyList()
                    _eventState.postValue(NetworkState.Success(events))
                } else {
                    val message = response.body()?.message ?: "Unknown error"
                    _eventState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _eventState.postValue(NetworkState.Error("Exception: ${e.localizedMessage}"))
            }
        }
    }

    // Join event
    fun joinEvent(userId: String, eventId: String) {
        _joinEventState.value = NetworkState.Loading

        viewModelScope.launch {
            try {
                val response: Response<ApiResponse> = repository.joinEvent(userId, eventId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _joinEventState.postValue(NetworkState.Success(response.body()!!))
                } else {
                    val message = response.body()?.message ?: "Unknown error"
                    _joinEventState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _joinEventState.postValue(NetworkState.Error("Exception: ${e.localizedMessage}"))
            }
        }
    }

    // Unjoin event
    fun unjoinEvent(userId: String, eventId: String) {
        _unjoinEventState.value = NetworkState.Loading

        viewModelScope.launch {
            try {
                val response: Response<ApiResponse> = repository.unjoinEvent(userId, eventId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _unjoinEventState.postValue(NetworkState.Success(response.body()!!))
                } else {
                    val message = response.body()?.message ?: "Unknown error"
                    _unjoinEventState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _unjoinEventState.postValue(NetworkState.Error("Exception: ${e.localizedMessage}"))
            }
        }
    }

    // Clear join event LiveData
    fun clearJoinEventState() {
        _joinEventState.value = null
    }

    // Clear unjoin event LiveData
    fun clearUnjoinEventState() {
        _unjoinEventState.value = null
    }

    // Fetch user details by ID
    fun fetchUserDetailsById(userId: String, page: Int, limit: Int) {
        _userDetailsState.value = NetworkState.Loading

        viewModelScope.launch {
            try {
                val response: Response<GetUsersResponse> = repository.getUserDetailsById(userId, page, limit)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val users = response.body()?.users ?: emptyList()
                    _userDetailsState.postValue(NetworkState.Success(users))
                } else {
                    val message = response.body()?.message ?: "Unknown error"
                    _userDetailsState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _userDetailsState.postValue(NetworkState.Error("Exception: ${e.localizedMessage}"))
            }
        }
    }

    // Fetch single event details by event ID (EventDetail)
    fun fetchEventDetailsById(userId: String, eventId: String) {
        _eventDetailsState.value = NetworkState.Loading

        viewModelScope.launch {
            try {
                val response: Response<com.tie.vibein.discover.data.models.EventDetailsResponse> =
                    repository.getEventDetailsById(userId, eventId)

                if (response.isSuccessful && response.body()?.status == "success") {
                    // response.body()?.event is of type EventDetail
                    _eventDetailsState.postValue(NetworkState.Success(response.body()?.event))
                } else {
                    val message = response.body()?.message ?: "Unknown error"
                    _eventDetailsState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _eventDetailsState.postValue(NetworkState.Error("Exception: ${e.localizedMessage}"))
            }
        }
    }
}
