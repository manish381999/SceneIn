package com.scenein.discover.presentation.view_model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenein.createEvent.data.models.ApiResponse
import com.scenein.createEvent.data.models.GetCategoryResponse
import com.scenein.createEvent.data.repository.CreateEventRepository
import com.scenein.discover.data.models.EventDetail
import com.scenein.discover.data.models.EventDetailResponse
import com.scenein.discover.data.models.FeedItem
import com.scenein.discover.data.models.Participant
import com.scenein.discover.data.models.SuggestedConnection
import com.scenein.discover.data.models.toEventSummary

import com.scenein.discover.data.repository.DiscoverRepository
import com.scenein.utils.NetworkState
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response

class DiscoverViewModel : ViewModel() {

    private val repository = DiscoverRepository()
    private val createEventRepository = CreateEventRepository() // Use the existing repository

    private val _feedState = MutableLiveData<NetworkState<List<FeedItem>>>()
    val feedState: LiveData<NetworkState<List<FeedItem>>> get() = _feedState

    private val _categoryState = MutableLiveData<NetworkState<GetCategoryResponse>>()
    val categoryState: LiveData<NetworkState<GetCategoryResponse>> get() = _categoryState


    // LiveData for joining event response
    private val _joinEventState = MutableLiveData<NetworkState<ApiResponse>?>()
    val joinEventState: LiveData<NetworkState<ApiResponse>?> get() = _joinEventState

    // LiveData for unjoining event response
    private val _unjoinEventState = MutableLiveData<NetworkState<ApiResponse>?>()
    val unjoinEventState: LiveData<NetworkState<ApiResponse>?> get() = _unjoinEventState


    // LiveData for single event detail (EventDetail)
    private val _eventDetailsState = MutableLiveData<NetworkState<EventDetail?>>()
    val eventDetailsState: LiveData<NetworkState<EventDetail?>> get() = _eventDetailsState

    private val _participantsState = MutableLiveData<NetworkState<List<Participant>>>()
    val participantsState: LiveData<NetworkState<List<Participant>>> get() = _participantsState

    private val _bookmarkState = MutableLiveData<NetworkState<String>?>()
    val bookmarkState: LiveData<NetworkState<String>?> get() = _bookmarkState


    var currentPage = 1
    private var isFetching = false
    private var allItemsLoaded = false
    private var currentCategoryId: String? = null
    var currentCity: String? = null


    init {
        fetchCategories()
    }

    fun fetchInitialFeed(city: String) {
        currentCity = city
        updateUserLocation(city)

        currentPage = 1
        allItemsLoaded = false
        fetchFeedPage()
    }

    fun fetchMoreFeedItems() {
        if (isFetching || allItemsLoaded) return
        currentPage++
        fetchFeedPage()
    }

    fun filterEventsByCategory(categoryId: String, city: String) {
        currentCategoryId = if (categoryId == "0") null else categoryId
        fetchInitialFeed(city)
    }

    private fun fetchFeedPage() {
        if (isFetching || allItemsLoaded) return
        isFetching = true

        if (currentPage == 1) {
            _feedState.value = NetworkState.Loading
        }

        viewModelScope.launch {
            try {
                val result = repository.getDiscoverFeed(currentPage, currentCategoryId)

                val currentList = if (currentPage == 1) mutableListOf() else (_feedState.value as? NetworkState.Success)?.data?.toMutableList() ?: mutableListOf()

                if (result.isEmpty() && currentPage > 1) {
                    allItemsLoaded = true
                }
                currentList.addAll(result)
                _feedState.postValue(NetworkState.Success(currentList))

            } catch (e: Exception) {
                _feedState.postValue(NetworkState.Error(e.message ?: "An unknown error occurred"))
            } finally {
                isFetching = false
            }
        }
    }

    private fun updateUserLocation(city: String) {
        viewModelScope.launch {
            try {
                repository.updateUserLocation(city)
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Failed to update location: ${e.message}")
            }
        }
    }

    private fun fetchCategories() {
        viewModelScope.launch {
            _categoryState.value = createEventRepository.fetchCategories()
        }
    }


    // Join event
    // In DiscoverViewModel.kt

    // In DiscoverViewModel.kt

    fun joinEvent(eventId: String) {
        _joinEventState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val joinResponse = repository.joinEvent(eventId)
                if (joinResponse.isSuccessful && joinResponse.body()?.status == "success") {
                    _joinEventState.postValue(NetworkState.Success(joinResponse.body()!!))

                    // --- THE FIX: Re-fetch the single event for fresh data ---
                    refreshSingleEventInFeed(eventId)

                } else {
                    val message = joinResponse.body()?.message ?: "Unknown error"
                    _joinEventState.postValue(NetworkState.Error(message))
                }
            } catch (e: Exception) {
                _joinEventState.postValue(NetworkState.Error("Exception: ${e.localizedMessage}"))
            }
        }
    }

    fun unjoinEvent(eventId: String) {
        _unjoinEventState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val unjoinResponse = repository.unjoinEvent(eventId)
                if (unjoinResponse.isSuccessful && unjoinResponse.body()?.status == "success") {
                    _unjoinEventState.postValue(NetworkState.Success(unjoinResponse.body()!!))

                    // --- THE FIX: Re-fetch the single event for fresh data ---
                    refreshSingleEventInFeed(eventId)

                } else {
                    val message = unjoinResponse.body()?.message ?: "Unknown error"
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

    fun refreshSingleEventInFeed(eventId: String) {
        viewModelScope.launch {
            try {
                // Re-fetch the full details of the event
                val updatedEventDetail = repository.getUpdatedEventDetails(eventId)
                // Convert it to the summary object your list uses
                val updatedEventSummary = updatedEventDetail.toEventSummary()

                // Update the local list with the new, accurate data
                val currentFeedState = _feedState.value
                if (currentFeedState is NetworkState.Success) {
                    val updatedList = currentFeedState.data.map { feedItem ->
                        if (feedItem is FeedItem.Event && feedItem.eventSummary.id == eventId) {
                            feedItem.copy(eventSummary = updatedEventSummary)
                        } else {
                            feedItem
                        }
                    }
                    _feedState.postValue(NetworkState.Success(updatedList))
                }
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Failed to refresh single event: ${e.message}")
            }
        }
    }

    fun updateConnectionStatusInFeed(connectionUserId: String, newStatus: String?, newSentBy: String?, newConnectionId: String?) {
        val currentFeedState = _feedState.value
        if (currentFeedState is NetworkState.Success) {

            // Create a new list by mapping over the old one
            val updatedList = currentFeedState.data.map { feedItem ->

                // Find the horizontal list of connections
                if (feedItem is FeedItem.Connections) {

                    // Create a new list of connections by mapping over the old one
                    val updatedConnections = feedItem.connections.map { connection ->

                        // Find the specific connection that was changed
                        if (connection.userId == connectionUserId) {
                            // Return a *copy* of the connection with the new status
                            connection.copy(
                                connectionStatus = newStatus,
                                requestSentBy = newSentBy,
                                connectionId = newConnectionId
                            )
                        } else {
                            connection // Return other connections unchanged
                        }
                    }
                    // Return a copy of the FeedItem with the updated connections list
                    feedItem.copy(connections = updatedConnections)
                } else {
                    feedItem // Return other feed items (events, headers) unchanged
                }
            }
            // Post the final, updated list to the UI
            _feedState.postValue(NetworkState.Success(updatedList))
        }
    }

    fun addBookmark(eventId: String) {
        viewModelScope.launch {
            // 1. Optimistic UI Update: Assume success and update the list immediately.
            updateBookmarkStatus(eventId, true)

            try {
                val response = repository.addBookmark(eventId)
                if (!response.isSuccessful) {
                    // 2. If API fails, revert the change.
                    updateBookmarkStatus(eventId, false)
                    _bookmarkState.postValue(NetworkState.Error("Failed to add bookmark"))
                }
            } catch (e: Exception) {
                // 3. If an exception occurs, also revert the change.
                updateBookmarkStatus(eventId, false)
                _bookmarkState.postValue(NetworkState.Error(e.message ?: "An error occurred"))
            }
        }
    }

    fun removeBookmark(eventId: String) {
        viewModelScope.launch {
            // 1. Optimistic UI Update
            updateBookmarkStatus(eventId, false)

            try {
                val response = repository.removeBookmark(eventId)
                if (!response.isSuccessful) {
                    // 2. Revert on failure
                    updateBookmarkStatus(eventId, true)
                    _bookmarkState.postValue(NetworkState.Error("Failed to remove bookmark"))
                }
            } catch (e: Exception) {
                // 3. Revert on failure
                updateBookmarkStatus(eventId, true)
                _bookmarkState.postValue(NetworkState.Error(e.message ?: "An error occurred"))
            }
        }
    }

    // Helper function to perform the local list mutation for optimistic updates
    private fun updateBookmarkStatus(eventId: String, isBookmarked: Boolean) {
        val currentFeedState = _feedState.value
        if (currentFeedState is NetworkState.Success) {
            val updatedList = currentFeedState.data.map { feedItem ->
                if (feedItem is FeedItem.Event && feedItem.eventSummary.id == eventId) {
                    // Create a copy with the new bookmarked status
                    val updatedSummary = feedItem.eventSummary.copy(hasBookmarked = isBookmarked)
                    feedItem.copy(eventSummary = updatedSummary)
                } else {
                    feedItem
                }
            }
            // Post the updated list to the UI
            _feedState.postValue(NetworkState.Success(updatedList))
        }
    }

    fun clearBookmarkState() {
        _bookmarkState.value = null
    }



    // Fetch single event details by event ID (EventDetail)
    fun fetchEventDetailsById(eventId: String) {
        _eventDetailsState.value = NetworkState.Loading

        viewModelScope.launch {
            try {
                val response: Response<EventDetailResponse> =
                    repository.getEventDetailsById(eventId)

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


    fun fetchEventParticipants( eventId: String, page: Int) {
        _participantsState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getEventParticipants(eventId, page)
                if (response.isSuccessful) {
                    _participantsState.postValue(NetworkState.Success(response.body()!!.participants))
                } else {
                    _participantsState.postValue(NetworkState.Error("Failed to load participants."))
                }
            } catch(e: Exception) {
                _participantsState.postValue(NetworkState.Error(e.message ?: "An error occurred."))
            }
        }
    }





}
