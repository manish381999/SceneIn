package com.scenein.discover.data.retrofit


import com.scenein.createEvent.data.models.ApiResponse
import com.scenein.discover.data.models.ConnectionsApiResponse
import com.scenein.discover.data.models.DiscoverApiResponse
import com.scenein.discover.data.models.EventDetailResponse

import com.scenein.discover.data.models.ParticipantsResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface DiscoverApiEndPoint {


    @FormUrlEncoded
    @POST("api_v1/get_events_by_city.php")
    suspend fun getEventsByCity(
        @Field("page") page: Int,
        @Field("category_id") categoryId: String? // Corrected to String
    ): Response<DiscoverApiResponse>

    @FormUrlEncoded
    @POST("api_v1/get_suggested_connections.php")
    suspend fun getSuggestedConnections(
        @Field("page") page: Int
    ): Response<ConnectionsApiResponse>

    @FormUrlEncoded
    @POST("api_v1/update_user_location.php")
    suspend fun updateUserLocation(
        @Field("city") city: String
    ): Response<ApiResponse>


    @FormUrlEncoded
    @POST("api_v1/JoinEventController.php")
    suspend fun joinEvent(
        @Field("event_id") eventId: String,
        @Field("action") action: String // 'join'
    ): Response<ApiResponse>

    @FormUrlEncoded
    @POST("api_v1/JoinEventController.php")
    suspend fun unjoinEvent(
        @Field("event_id") eventId: String,
        @Field("action") action: String // 'unjoin'
    ): Response<ApiResponse>


    @FormUrlEncoded
    @POST("api_v1/get_event_details_by_id.php")
    suspend fun getEventDetailsById(
        @Field("event_id") eventId: String
    ): Response<EventDetailResponse>

    @FormUrlEncoded
    @POST("api_v1/get_event_participants.php")
    suspend fun getEventParticipants(
        @Field("event_id") eventId: String,
        @Field("page") page: Int
    ): Response<ParticipantsResponse>


    @FormUrlEncoded
    @POST("api_v1/add_bookmark.php")
    suspend fun addBookmark(@Field("event_id") eventId: String): Response<ApiResponse>

    @FormUrlEncoded
    @POST("api_v1/remove_bookmark.php")
    suspend fun removeBookmark(@Field("event_id") eventId: String): Response<ApiResponse>


}