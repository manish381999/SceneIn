package com.tie.vibein.services

import android.util.Log
import com.tie.vibein.credentials.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FcmTokenManager(private val repository: AuthRepository) {

    fun updateFcmToken(userId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = repository.updateFcmToken(userId, token)
                if (response.isSuccessful) {
                    Log.d("FCM", "FCM token updated successfully via repository")
                } else {
                    Log.e("FCM", "Failed to update token: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Exception while updating token: ${e.message}")
            }
        }
    }
}
