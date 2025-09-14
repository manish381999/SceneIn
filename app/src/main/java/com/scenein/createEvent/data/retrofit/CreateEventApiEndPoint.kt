package com.scenein.createEvent.data.retrofit


import com.scenein.createEvent.data.models.AgeRestrictionResponse
import com.scenein.createEvent.data.models.ApiResponse
import com.scenein.createEvent.data.models.GetCategoryResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface CreateEventApiEndPoint {

    @GET("common/get_categories")
    suspend fun getCategories(): Response<GetCategoryResponse>


    @GET("common/get_age_restrictions")
    suspend fun getAgeRestriction(): Response<AgeRestrictionResponse>

    @Multipart
    @POST("events/create_event")
    suspend fun createEvent(
        // This is the change: Accept the whole map of text data
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>,

        // The image part remains the same
        @Part cover_image: MultipartBody.Part?
    ): Response<ApiResponse>



}