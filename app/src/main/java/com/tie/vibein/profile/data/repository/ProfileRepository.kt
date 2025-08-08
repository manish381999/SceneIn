package com.tie.vibein.profile.data.repository

import com.tie.vibein.credentials.data.retrofit.RetrofitClient
import com.tie.vibein.profile.data.models.ConnectionActionResponse
import com.tie.vibein.profile.data.models.ConnectionStatusResponse
import com.tie.vibein.profile.data.models.GetEventsByUserResponse
import com.tie.vibein.profile.data.models.GetUserResponse
import retrofit2.Response

class ProfileRepository {

    private val api = RetrofitClient.profileApiEndPoint



    // === NEW: Repository function for the private profile ===
    suspend fun getMyProfile(userId: String) = api.getMyProfile(userId)

    // === UPDATED: Replaces getEventsByUser ===
    suspend fun getPublicUserProfile(userId: String): Response<GetUserResponse> {
        return api.getPublicUserProfile(userId)
    }

    suspend fun getEventsByUser(userId: String): Response<GetEventsByUserResponse> {
        return api.getEventsByUser(userId)
    }

    suspend fun checkConnectionStatus(viewerId: String, profileId: String): Response<ConnectionStatusResponse> {
        return api.checkConnectionStatus(viewerId, profileId)
    }

    suspend fun sendConnectionRequest(senderId: String, receiverId: String): Response<ConnectionActionResponse> {
        return api.sendConnectionRequest(senderId, receiverId)
    }

    suspend fun removeConnection(currentUserId: String, userToDisconnectId: String): Response<ConnectionActionResponse> {
        return api.removeConnection(currentUserId, userToDisconnectId)
    }
    suspend fun respondToConnectionRequest(connectionId: String, response: String): Response<ConnectionActionResponse> {
        return api.respondToConnectionRequest(connectionId, response)
    }

    suspend fun getMyTicketsActivity(userId: String) = api.getMyTicketsActivity(userId)

    // --- NEW: Repository function for the "Connections" tab ---
    suspend fun getMyConnections(userId: String) = api.getMyConnections(userId)
}