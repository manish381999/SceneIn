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

    @GET("api_v1/getCategories.php")
    suspend fun getCategories(): Response<GetCategoryResponse>


    @GET("api_v1/get_age_restriction.php")
    suspend fun getAgeRestriction(): Response<AgeRestrictionResponse>

    @Multipart
    @POST("api_v1/create_event.php")
    suspend fun createEvent(
        // This is the change: Accept the whole map of text data
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>,

        // The image part remains the same
        @Part cover_image: MultipartBody.Part?
    ): Response<ApiResponse>



}