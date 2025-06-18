package com.tie.vibein.createEvent.data.repository

import com.tie.vibein.createEvent.data.models.AgeRestrictionResponse
import com.tie.vibein.createEvent.data.models.ApiResponse
import com.tie.vibein.createEvent.data.models.GetCategoryResponse
import com.tie.vibein.credentials.data.retrofit.RetrofitClient
import com.tie.vibein.utils.NetworkState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody

class CreateEventRepository {

    private val api = RetrofitClient.createEventApiEndPoint

    suspend fun fetchCategories(): NetworkState<GetCategoryResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCategories()
                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    NetworkState.Error("API Error: ${response.message()}")
                }
            } catch (e: Exception) {
                NetworkState.Error("Exception: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    suspend fun fetchAgeRestrictions(): NetworkState<AgeRestrictionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getAgeRestriction()
                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    NetworkState.Error("API Error: ${response.message()}")
                }
            } catch (e: Exception) {
                NetworkState.Error("Exception: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    suspend fun createEvent(
        params: Map<String, @JvmSuppressWildcards RequestBody>,
        cover_image: MultipartBody.Part?
    ): NetworkState<ApiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.createEvent(
                    userId = params["user_id"]!!,
                    eventName = params["event_name"]!!,
                    categoryId = params["category_id"]!!,
                    eventDescription = params["event_description"]!!,
                    eventDate = params["event_date"]!!,
                    startTime = params["start_time"]!!,
                    endTime = params["end_time"]!!,
                    eventDeliveryMode = params["eventDeliveryMode"]!!,
                    venueLocation = params["venueLocation"]!!,
                    meetingLink = params["meetingLink"]!!,
                    maxParticipants = params["maximum_participants"]!!,
                    selectedAgeId = params["selectedAgeId"]!!,
                    selectedEventType = params["selectedEventType"]!!,
                    ticketPrice = params["ticketPrice"]!!,
                    pincode = params["pincode"]!!,
                    city = params["city"]!!,
                    full_address = params["full_address"]!!,
                    cover_image = cover_image
                )

                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    NetworkState.Error("Create Event Failed: ${response.message()}")
                }
            } catch (e: Exception) {
                NetworkState.Error("Exception: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }
}
