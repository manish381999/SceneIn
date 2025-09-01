package com.scenein.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val api: PincodeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.postalpincode.in/") // Base URL ends with /
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PincodeApiService::class.java)
    }
}