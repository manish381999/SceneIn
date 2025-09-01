package com.scenein.credentials.data.retrofit

import android.content.Context
import com.scenein.utils.SP
import okhttp3.Interceptor
import okhttp3.Response

/**
 * This interceptor automatically adds the user's saved authentication token
 * to the headers of every outgoing API request.
 */
class AuthInterceptor(context: Context) : Interceptor {
    // Use application context to avoid memory leaks
    private val appContext = context.applicationContext

    override fun intercept(chain: Interceptor.Chain): Response {
        // Retrieve the saved auth_token from SharedPreferences.
        val authToken = SP.getString(appContext, SP.AUTH_TOKEN)

        // Get the original request that the app is trying to send.
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // If the token exists, add it to the request as an "Authorization" header.
        // The "Bearer" scheme is the industry standard.
        if (!authToken.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $authToken")
        }

        val newRequest = requestBuilder.build()

        // Proceed with the new (potentially authenticated) request.
        return chain.proceed(newRequest)
    }
}