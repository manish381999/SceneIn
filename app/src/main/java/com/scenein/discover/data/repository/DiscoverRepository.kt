package com.scenein.discover.data.repository


import android.util.Log
import com.scenein.createEvent.data.models.ApiResponse
import com.scenein.credentials.data.retrofit.RetrofitClient
import com.scenein.discover.data.models.EventDetail
import com.scenein.discover.data.models.EventDetailResponse
import com.scenein.discover.data.models.FeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

import retrofit2.Response

class DiscoverRepository {

    private val api = RetrofitClient.discoverApiEndPoint


    /**
     * Fetches the combined feed and returns the raw list or throws an exception.
     */
    suspend fun getDiscoverFeed(page: Int, categoryId: String?): List<FeedItem> {
        return withContext(Dispatchers.IO) {
            val eventsDeferred = async { api.getEventsByCity(page, categoryId) }
            val connectionsDeferred = async {
                if (page == 1 && categoryId == null) {
                    api.getSuggestedConnections(1)
                } else {
                    null
                }
            }

            val eventsResponse = eventsDeferred.await()
            val connectionsResponse = connectionsDeferred.await()

            if (!eventsResponse.isSuccessful) {
                throw Exception("Failed to fetch events: ${eventsResponse.message()}")
            }

            val eventList = eventsResponse.body()?.events ?: emptyList()
            val connectionList = connectionsResponse?.body()?.connections ?: emptyList()

            // --- Build the Combined Feed ---
            val feedItems = mutableListOf<FeedItem>()
            eventList.forEachIndexed { index, event ->
                feedItems.add(FeedItem.Event(event))
                // Intersperse connections after the 2nd event on the first page
                if (page == 1 && index == 1 && connectionList.isNotEmpty()) {
                    // --- THIS IS THE CORRECTED LINE ---
                    // We now correctly provide the title and the list of connections.
                    feedItems.add(FeedItem.Connections(
                        title = "People You May Know",
                        connections = connectionList
                    ))
                }
            }

            feedItems // Return the raw list
        }
    }

    /**
     * Updates the user's location and returns the response or throws an exception.
     */
    suspend fun updateUserLocation(city: String): ApiResponse {
        return withContext(Dispatchers.IO) {
            val response = api.updateUserLocation(city)
            if (response.isSuccessful && response.body() != null) {
                Log.d("DiscoverRepository", "Location updated: ${response.body()!!.message}")
                response.body()!!
            } else {
                throw Exception("Failed to update location: ${response.message()}")
            }
        }
    }





    suspend fun joinEvent(eventId: String): Response<ApiResponse> {
        return api.joinEvent(eventId, "join")
    }

    suspend fun unjoinEvent(eventId: String): Response<ApiResponse> {
        return api.unjoinEvent( eventId, "unjoin")
    }

    suspend fun getUpdatedEventDetails(eventId: String): EventDetail {
        val response = api.getEventDetailsById(eventId) // Uses your existing Retrofit call
        if (response.isSuccessful && response.body()?.status == "success") {
            // Make sure to handle the case where 'event' could be null
            return response.body()!!.event ?: throw Exception("Event data was null in response")
        } else {
            throw Exception("API call to get event details failed")
        }
    }

    suspend fun getEventDetailsById(
        eventId: String
    ): Response<EventDetailResponse> {
        return api.getEventDetailsById(eventId)
    }

    suspend fun getEventParticipants( eventId: String, page: Int) =
        api.getEventParticipants( eventId, page)


    suspend fun addBookmark(eventId: String): Response<ApiResponse> {
        return api.addBookmark(eventId)
    }

    suspend fun removeBookmark(eventId: String): Response<ApiResponse> {
        return api.removeBookmark(eventId)
    }
}
