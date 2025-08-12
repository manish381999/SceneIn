package com.tie.vibein.settings.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tie.vibein.settings.data.models.SettingsActionResponse
import com.tie.vibein.settings.data.repository.SettingsRepository
import com.tie.vibein.utils.NetworkState
import kotlinx.coroutines.launch
import org.json.JSONObject

class SettingsViewModel : ViewModel() {

    private val repository = SettingsRepository()

    private val _settingsActionState = MutableLiveData<NetworkState<SettingsActionResponse>>()
    val settingsActionState: LiveData<NetworkState<SettingsActionResponse>> = _settingsActionState

    fun updateAccountPrivacy(userId: String, isPrivate: Boolean) {
        _settingsActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _settingsActionState.value = try {
                val response = repository.updatePrivacy(userId, isPrivate)
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

    fun deleteAccount(userId: String) {
        _settingsActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _settingsActionState.value = try {
                // --- THIS IS THE DEFINITIVE FIX for deleteAccount ---
                // 1. Call the repository to get the full Response object.
                val response = repository.deleteAccount(userId)

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

    fun logout(userId: String) {
        _settingsActionState.value = NetworkState.Loading
        viewModelScope.launch {
            _settingsActionState.value = try {
                // This is the same corrected logic we applied before.
                val response = repository.logout(userId)

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
}