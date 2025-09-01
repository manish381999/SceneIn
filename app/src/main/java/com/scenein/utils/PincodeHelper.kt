package com.scenein.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object PincodeHelper {

    fun fetchCityFromPincode(pincode: String, onResult: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.getCityByPincode(pincode)
                if (response.isSuccessful && response.body() != null) {
                    val postOffices = response.body()!![0].PostOffice
                    val city = postOffices?.firstOrNull()?.District
                    withContext(Dispatchers.Main) {
                        onResult(city)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                Log.e("PincodeHelper", "Error fetching city: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult(null)
                }
            }
        }
    }
}
