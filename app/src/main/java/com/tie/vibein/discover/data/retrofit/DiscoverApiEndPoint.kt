package com.tie.vibein.discover.data.retrofit

import com.tie.vibein.createEvent.data.models.ApiResponse
import com.tie.vibein.discover.data.model.GetEventsResponse
import com.tie.vibein.discover.data.models.EventDetailsResponse
import com.tie.vibein.discover.data.models.GetUsersResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface DiscoverApiEndPoint {

    @FormUrlEncoded
    @POST("api_v1/get_events_by_city.php")
    suspend fun getCityEvents(
        @Field("user_id") userId: String,
        @Field("city") city: String,
        @Field("current_date") currentDate: String
    ): Response<GetEventsResponse>

    @FormUrlEncoded
    @POST("api_v1/JoinEventController.php")
    suspend fun joinEvent(
        @Field("user_id") userId: String,
        @Field("event_id") eventId: String,
        @Field("action") action: String // 'join'
    ): Response<ApiResponse>

    @FormUrlEncoded
    @POST("api_v1/JoinEventController.php")
    suspend fun unjoinEvent(
        @Field("user_id") userId: String,
        @Field("event_id") eventId: String,
        @Field("action") action: String // 'unjoin'
    ): Response<ApiResponse>


    @FormUrlEncoded
    @POST("api_v1/get_event_details_by_id.php")
    suspend fun getEventDetailsById(
        @Field("user_id") userId: String,
        @Field("event_id") eventId: String
    ): Response<EventDetailsResponse>

    @FormUrlEncoded
    @POST("api_v1/get_user_details_by_id.php")
    suspend fun getUserDetailsById(
        @Field("user_id") userId: String, // Can be single or comma-separated multiple user ids
        @Field("page") page: Int,
        @Field("limit") limit: Int
    ): Response<GetUsersResponse>
}