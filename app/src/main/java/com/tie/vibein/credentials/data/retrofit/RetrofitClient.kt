package com.tie.vibein.credentials.data.retrofit

import com.tie.vibein.chat.data.retrofit.ChatApiEndPoint
import com.tie.vibein.createEvent.data.retrofit.CreateEventApiEndPoint
import com.tie.vibein.discover.data.retrofit.DiscoverApiEndPoint
import com.tie.vibein.notification.data.retrofit.NotificationApiEndPoint
import com.tie.vibein.profile.data.retrofit.ProfileApiEndPoint
import com.tie.vibein.tickets.data.retrofit.TicketApiEndPoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://dreamsquad.fun/" // Replace with your base URL

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val createEventApiEndPoint: CreateEventApiEndPoint by lazy {
        retrofit.create(CreateEventApiEndPoint::class.java)
    }

    val profileApiEndPoint: ProfileApiEndPoint by lazy {
        retrofit.create(ProfileApiEndPoint::class.java)
    }

    val discoverApiEndPoint: DiscoverApiEndPoint by lazy {
        retrofit.create(DiscoverApiEndPoint::class.java)
    }
    // In RetrofitClient.kt
    val chatApiEndPoint: ChatApiEndPoint by lazy {
        retrofit.create(ChatApiEndPoint::class.java)
    }

    val ticketApiEndPoint: TicketApiEndPoint by lazy {
        retrofit.create(TicketApiEndPoint::class.java)
    }

    val notificationApiEndPoint: NotificationApiEndPoint by lazy {
        retrofit.create(NotificationApiEndPoint::class.java)
    }
}