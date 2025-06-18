package com.tie.vibein.createEvent.data.retrofit

import com.tie.vibein.createEvent.data.models.AgeRestrictionResponse
import com.tie.vibein.createEvent.data.models.ApiResponse
import com.tie.vibein.createEvent.data.models.GetCategoryResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CreateEventApiEndPoint {

    @GET("api_v1/getCategories.php")
    suspend fun getCategories(): Response<GetCategoryResponse>


    @GET("api_v1/get_age_restriction.php")
    suspend fun getAgeRestriction(): Response<AgeRestrictionResponse>

    @Multipart
    @POST("api_v1/create_event.php")
    suspend fun createEvent(
        @Part("user_id") userId: RequestBody,
        @Part("event_name") eventName: RequestBody,
        @Part("category_id") categoryId: RequestBody,
        @Part("event_description") eventDescription: RequestBody,
        @Part("event_date") eventDate: RequestBody,
        @Part("start_time") startTime: RequestBody,
        @Part("end_time") endTime: RequestBody,
        @Part("eventDeliveryMode") eventDeliveryMode: RequestBody,
        @Part("venueLocation") venueLocation: RequestBody,
        @Part("meetingLink") meetingLink: RequestBody,
        @Part("maximum_participants") maxParticipants: RequestBody,
        @Part("selectedAgeId") selectedAgeId: RequestBody,
        @Part("selectedEventType") selectedEventType: RequestBody,
        @Part("ticketPrice") ticketPrice: RequestBody,
        @Part("pincode") pincode: RequestBody,
        @Part("city") city: RequestBody,
        @Part("full_address") full_address: RequestBody,
        @Part cover_image: MultipartBody.Part?
    ): Response<ApiResponse>



}