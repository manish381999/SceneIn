package com.tie.vibein.utils

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface PincodeApiService {
    @GET("pincode/{pincode}")
    suspend fun getCityByPincode(@Path("pincode") pincode: String): Response<List<PincodeResponse>>
}