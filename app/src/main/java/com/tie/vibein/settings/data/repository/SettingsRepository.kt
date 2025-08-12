package com.tie.vibein.settings.data.repository

import com.tie.vibein.credentials.data.retrofit.RetrofitClient // Your existing client
import retrofit2.Response

class SettingsRepository {

    private val api = RetrofitClient.settingsApiEndPoint // We'll add this to RetrofitClient

    suspend fun updatePrivacy(userId: String, isPrivate: Boolean) =
        api.updatePrivacy(userId, if (isPrivate) 1 else 0)

    suspend fun deleteAccount(userId: String) =
        api.deleteAccount(userId)

    suspend fun logout(userId: String) =
        api.logout(userId)
}