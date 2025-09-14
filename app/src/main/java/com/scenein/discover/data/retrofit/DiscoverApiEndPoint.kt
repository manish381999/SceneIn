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
    @POST("events/by_city")
    suspend fun getEventsByCity(
        @Field("page") page: Int,
        @Field("category_id") categoryId: String? // Corrected to String
    ): Response<DiscoverApiResponse>

    @FormUrlEncoded
    @POST("connections/suggestions")
    suspend fun getSuggestedConnections(
        @Field("page") page: Int
    ): Response<ConnectionsApiResponse>

    @FormUrlEncoded
    @POST("users/update_location")
    suspend fun updateUserLocation(
        @Field("city") city: String
    ): Response<ApiResponse>


    @FormUrlEncoded
    @POST("events/join_or_unjoin")
    suspend fun joinEvent(
        @Field("event_id") eventId: String,
        @Field("action") action: String // 'join'
    ): Response<ApiResponse>

    @FormUrlEncoded
    @POST("events/join_or_unjoin")
    suspend fun unjoinEvent(
        @Field("event_id") eventId: String,
        @Field("action") action: String // 'unjoin'
    ): Response<ApiResponse>


    @FormUrlEncoded
    @POST("events/details")
    suspend fun getEventDetailsById(
        @Field("event_id") eventId: String
    ): Response<EventDetailResponse>

    @FormUrlEncoded
    @POST("events/get_participants")
    suspend fun getEventParticipants(
        @Field("event_id") eventId: String,
        @Field("page") page: Int
    ): Response<ParticipantsResponse>


    @FormUrlEncoded
    @POST("bookmarks/add")
    suspend fun addBookmark(@Field("event_id") eventId: String): Response<ApiResponse>

    @FormUrlEncoded
    @POST("bookmarks/remove")
    suspend fun removeBookmark(@Field("event_id") eventId: String): Response<ApiResponse>


}