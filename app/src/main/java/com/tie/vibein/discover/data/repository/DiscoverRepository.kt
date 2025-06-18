package com.tie.vibein.discover.data.repository

import com.tie.vibein.createEvent.data.models.ApiResponse
import com.tie.vibein.credentials.data.retrofit.RetrofitClient
import com.tie.vibein.discover.data.model.GetEventsResponse
import com.tie.vibein.discover.data.models.EventDetailsResponse
import com.tie.vibein.discover.data.models.GetUsersResponse
import retrofit2.Response

class DiscoverRepository {

    private val api = RetrofitClient.discoverApiEndPoint

    suspend fun getEventsByCity(
        userId: String,
        city: String,
        currentDate: String
    ): Response<GetEventsResponse> {
        return api.getCityEvents(userId, city, currentDate)
    }

    suspend fun joinEvent(userId: String, eventId: String): Response<ApiResponse> {
        return api.joinEvent(userId, eventId, "join")
    }

    suspend fun unjoinEvent(userId: String, eventId: String): Response<ApiResponse> {
        return api.unjoinEvent(userId, eventId, "unjoin")
    }

    suspend fun getEventDetailsById(
        userId: String,
        eventId: String
    ): Response<EventDetailsResponse> {
        return api.getEventDetailsById(userId, eventId)
    }

    suspend fun getUserDetailsById(
        userId: String,
        page: Int,
        limit: Int
    ): Response<GetUsersResponse> {
        return api.getUserDetailsById(userId, page, limit)
    }
}
