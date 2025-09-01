package com.scenein.profile.data.repository

import com.scenein.credentials.data.retrofit.RetrofitClient
import com.scenein.profile.data.models.ConnectionActionResponse
import com.scenein.profile.data.models.ConnectionStatusResponse
import com.scenein.profile.data.models.GetEventsByUserResponse
import com.scenein.profile.data.models.GetUserResponse
import retrofit2.Response

class ProfileRepository {

    private val api = RetrofitClient.profileApiEndPoint



    // === NEW: Repository function for the private profile ===
    suspend fun getMyProfile() = api.getMyProfile()

    // === UPDATED: Replaces getEventsByUser ===
    suspend fun getPublicUserProfile(profileId: String): Response<GetUserResponse> {
        return api.getPublicUserProfile(profileId)
    }


    suspend fun getEventsByUser(userId: String?): Response<GetEventsByUserResponse> {
        return api.getEventsByUser(userId)
    }

    suspend fun checkConnectionStatus(profileId: String): Response<ConnectionStatusResponse> {
        return api.checkConnectionStatus( profileId)
    }

    suspend fun sendConnectionRequest( receiverId: String): Response<ConnectionActionResponse> {
        return api.sendConnectionRequest( receiverId)
    }

    suspend fun removeConnection(otherUserId: String): Response<ConnectionActionResponse> {
        return api.removeConnection(otherUserId)
    }
    suspend fun respondToConnectionRequest(connectionId: String, response: String): Response<ConnectionActionResponse> {
        return api.respondToConnectionRequest(connectionId, response)
    }

    suspend fun getMyTicketsActivity() = api.getMyTicketsActivity()

    // --- NEW: Repository function for the "Connections" tab ---
    suspend fun getMyConnections(userId: String?) = api.getMyConnections(userId)
}