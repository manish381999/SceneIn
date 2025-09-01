package com.scenein.createEvent.data.repository

import com.scenein.createEvent.data.models.AgeRestrictionResponse
import com.scenein.createEvent.data.models.ApiResponse
import com.scenein.createEvent.data.models.GetCategoryResponse
import com.scenein.credentials.data.retrofit.RetrofitClient
import com.scenein.utils.NetworkState
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
                // This is now much simpler and safer. We just pass the data through.
                val response = api.createEvent(
                    params = params,
                    cover_image = cover_image
                )

                if (response.isSuccessful && response.body() != null) {
                    NetworkState.Success(response.body()!!)
                } else {
                    // We can now get a more descriptive error from the server body
                    val errorBody = response.errorBody()?.string()
                    NetworkState.Error("API Error: ${response.code()} - ${errorBody ?: response.message()}")
                }
            } catch (e: Exception) {
                NetworkState.Error("Exception: ${e.message ?: "Unknown error"}")
            }
        }
    }
}
