package com.scenein.notification.presentation.view_model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenein.notification.data.respository.NotificationRepository
import com.scenein.notification.data.models.ActivityFeedResponse
import com.scenein.utils.NetworkState
import kotlinx.coroutines.launch
import org.json.JSONObject

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()

    // --- THIS IS THE CORRECTED LIVEDATA ---
    // It now correctly holds the entire ActivityFeedResponse object.
    private val _notificationState = MutableLiveData<NetworkState<ActivityFeedResponse>>()
    val notificationState: LiveData<NetworkState<ActivityFeedResponse>> = _notificationState

    fun fetchNotifications() {
        _notificationState.value = NetworkState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getNotifications()
                if (response.isSuccessful && response.body()?.status == "success") {
                    _notificationState.postValue(NetworkState.Success(response.body()!!))
                } else {
                    val errorMsg = JSONObject(response.errorBody()!!.string()).getString("message")
                    _notificationState.postValue(NetworkState.Error(errorMsg))
                }
            } catch (e: Exception) {
                _notificationState.postValue(NetworkState.Error(e.message ?: "An unknown error occurred."))
            }
        }
    }

    fun markNotificationsAsRead() {
        viewModelScope.launch {
            try {
                // This is a "fire and forget" call, so we don't post to LiveData.
                repository.markNotificationsAsRead()
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Failed to mark notifications as read", e)
            }
        }
    }
}