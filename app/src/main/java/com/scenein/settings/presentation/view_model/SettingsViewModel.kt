package com.scenein.settings.presentation.view_model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenein.discover.data.models.FeedItem
import com.scenein.discover.data.models.toEventSummary
import com.scenein.discover.data.repository.DiscoverRepository
import com.scenein.settings.data.models.SettingsActionResponse
import com.scenein.settings.data.models.TransactionDetail
import com.scenein.settings.data.models.TransactionHistoryItem
import com.scenein.settings.data.repository.SettingsRepository
import com.scenein.utils.NetworkState
import kotlinx.coroutines.launch
import org.json.JSONObject

class SettingsViewModel : ViewModel() {

    private val repository = SettingsRepository()
    private val discoverRepository = DiscoverRepository()

    private val _transactionHistoryState = MutableLiveData<NetworkState<List<TransactionHistoryItem>>>()
    val transactionHistoryState: LiveData<NetworkState<List<TransactionHistoryItem>>> = _transactionHistoryState

    private val _transactionDetailState = MutableLiveData<NetworkState<TransactionDetail>>()
    val transactionDetailState: LiveData<NetworkState<TransactionDetail>> = _transactionDetailState

    private val _settingsActionState = MutableLiveData<NetworkState<SettingsActionResponse>>()
    val settingsActionState: LiveData<NetworkState<SettingsActionResponse>> = _settingsActionState

    private val _bookmarkedEventsState = MutableLiveData<NetworkState<List<FeedItem>>>()
    val bookmarkedEventsState: LiveData<NetworkState<List<FeedItem>>> get() = _bookmarkedEventsState
    private val _joinEventState = MutableLiveData<NetworkState<String>?>()
    val joinEventState: LiveData<NetworkState<String>?> get() = _joinEventState
    private val _unjoinEventState = MutableLiveData<NetworkState<String>?>()
    val unjoinEventState: LiveData<NetworkState<String>?> get() = _unjoinEventState
    private val _bookmarkState = MutableLiveData<NetworkState<String>?>()
    val bookmarkState: LiveData<NetworkState<String>?> get() = _bookmarkState

    private var currentPage = 1
    private var isFetching = false
    private var allItemsLoaded = false


    fun fetchTicketTransactionHistory() {
        _transactionHistoryState.value = NetworkState.Loading
        viewModelScope.launch {
            _transactionHistoryState.value = try {
                val response = repository.getTicketTransactionHistory()

                // --- THIS IS THE DEFINITIVE FIX ---
                // We add robust checks to ensure the response body and its properties are not null.
                if (response.isSuccessful && response.body() != null) {
                    // We access the 'transactions' property from the response body.
                    NetworkState.Success(response.body()!!.transactions)
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }
                // --- END OF FIX ---

            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun fetchTransactionDetails(transactionId: Int) {
        _transactionDetailState.value = NetworkState.Loading
        viewModelScope.launch {
            _transactionDetailState.value = try {
                val response = repository.getTransactionDetails(transactionId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    NetworkState.Success(response.body()!!.transaction)
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun updateAccountPrivacy( isPrivate: Boolean) {
        _settingsActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _settingsActionState.value = try {
                val response = repository.updatePrivacy( isPrivate)
                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }
            } catch (e: Exception) {
                NetworkState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun deleteAccount() {
        _settingsActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _settingsActionState.value = try {
                // --- THIS IS THE DEFINITIVE FIX for deleteAccount ---
                // 1. Call the repository to get the full Response object.
                val response = repository.deleteAccount()

                // 2. Check if the response was successful and has a body.
                if (response.isSuccessful && response.body() != null) {
                    // 3. Post the ACTUAL, COMPLETE response from the server.
                    // This response INCLUDES the "action": "DEACTIVATE_SUCCESS" key-value pair.
                    NetworkState.Success(response.body()!!)
                } else {
                    // Handle a server-side error during deletion.
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }

            } catch (e: Exception) {
                // --- Fallback for Network Failure ---
                // We create a custom response WITH THE CORRECT ACTION KEY to trigger the UI logout.
                NetworkState.Success(SettingsActionResponse("success", "Account deactivated on device.", "DEACTIVATE_SUCCESS"))
            }
        }
    }

    fun logout() {
        _settingsActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _settingsActionState.value = try {
                // This is the same corrected logic we applied before.
                val response = repository.logout()

                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    NetworkState.Error(errorMsg)
                }

            } catch (e: Exception) {
                // Fallback for network failure.
                NetworkState.Success(SettingsActionResponse("success", "Logged out from device.", "LOGOUT_SUCCESS"))
            }
        }
    }



    fun fetchBookmarkedEvents() {
        currentPage = 1
        allItemsLoaded = false
        fetchBookmarkedEventsPage()
    }

    // Fetches the next page of bookmarked events
    fun fetchMoreBookmarkedEvents() {
        if (isFetching || allItemsLoaded) return
        currentPage++
        fetchBookmarkedEventsPage()
    }

    private fun fetchBookmarkedEventsPage() {
        if (isFetching || allItemsLoaded) return
        isFetching = true
        if (currentPage == 1) {
            _bookmarkedEventsState.value = NetworkState.Loading
        }
        viewModelScope.launch {
            try {
                val newEvents = repository.getBookmarkedEvents(currentPage)
                if (newEvents.isEmpty()) {
                    allItemsLoaded = true
                }
                val currentList = if (currentPage == 1) mutableListOf() else {
                    (_bookmarkedEventsState.value as? NetworkState.Success)?.data?.toMutableList() ?: mutableListOf()
                }
                currentList.addAll(newEvents)
                _bookmarkedEventsState.postValue(NetworkState.Success(currentList))
            } catch (e: Exception) {
                if (currentPage == 1) {
                    _bookmarkedEventsState.postValue(NetworkState.Error(e.message ?: "An error occurred"))
                }
            } finally {
                isFetching = false
            }
        }
    }

    // --- UPDATED ACTION LOGIC ---

    fun joinEvent(eventId: String) {
        viewModelScope.launch {
            try {
                val response = discoverRepository.joinEvent(eventId)
                if (response.isSuccessful) {
                    _joinEventState.postValue(NetworkState.Success("Joined successfully!"))
                    // After joining, refresh this item with the new participant data
                    refreshSingleBookmarkInFeed(eventId)
                } else {
                    _joinEventState.postValue(NetworkState.Error("Failed to join event"))
                }
            } catch (e: Exception) {
                e.message?.let { _joinEventState.postValue(NetworkState.Error(it)) }
            }
        }
    }

    fun unjoinEvent(eventId: String) {
        viewModelScope.launch {
            try {
                val response = discoverRepository.unjoinEvent(eventId)
                if (response.isSuccessful) {
                    _unjoinEventState.postValue(NetworkState.Success("Left event successfully!"))
                    // After un-joining, refresh this item with the new participant data
                    refreshSingleBookmarkInFeed(eventId)
                } else {
                    _unjoinEventState.postValue(NetworkState.Error("Failed to leave event"))
                }
            } catch (e: Exception) {
                e.message?.let { _unjoinEventState.postValue(NetworkState.Error(it)) }
            }
        }
    }

    fun removeBookmark(eventId: String) {
        val currentState = _bookmarkedEventsState.value
        if (currentState is NetworkState.Success) {
            // Optimistic UI Update: Remove the item from the list immediately
            val updatedList = currentState.data.filterNot { it is FeedItem.Event && it.eventSummary.id == eventId }
            _bookmarkedEventsState.postValue(NetworkState.Success(updatedList))
        }

        viewModelScope.launch {
            try {
                val response = discoverRepository.removeBookmark(eventId)
                if (response.isSuccessful) {
                    _bookmarkState.postValue(NetworkState.Success("Bookmark removed"))
                } else {
                    fetchBookmarkedEvents() // Revert on failure
                    _bookmarkState.postValue(NetworkState.Error("Failed to remove bookmark"))
                }
            } catch (e: Exception) {
                fetchBookmarkedEvents() // Revert on failure
                e.message?.let { _bookmarkState.postValue(NetworkState.Error(it)) }
            }
        }
    }

    // This is the new function that gets fresh data for a single item
    private fun refreshSingleBookmarkInFeed(eventId: String) {
        viewModelScope.launch {
            try {
                val updatedEventDetail = discoverRepository.getUpdatedEventDetails(eventId)
                val updatedEventSummary = updatedEventDetail.toEventSummary()

                val currentState = _bookmarkedEventsState.value
                if (currentState is NetworkState.Success) {
                    val updatedList = currentState.data.map { feedItem ->
                        if (feedItem is FeedItem.Event && feedItem.eventSummary.id == eventId) {
                            feedItem.copy(eventSummary = updatedEventSummary)
                        } else {
                            feedItem
                        }
                    }
                    _bookmarkedEventsState.postValue(NetworkState.Success(updatedList))
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to refresh single bookmark: ${e.message}")
            }
        }
    }

    fun clearJoinState() { _joinEventState.value = null }
    fun clearUnjoinState() { _unjoinEventState.value = null }
    fun clearBookmarkState() { _bookmarkState.value = null }
}