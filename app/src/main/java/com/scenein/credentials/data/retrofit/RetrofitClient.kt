package com.scenein.credentials.data.retrofit

import android.content.Context
import com.scenein.chat.data.retrofit.ChatApiEndPoint
import com.scenein.createEvent.data.retrofit.CreateEventApiEndPoint
import com.scenein.discover.data.retrofit.DiscoverApiEndPoint
import com.scenein.notification.data.retrofit.NotificationApiEndPoint
import com.scenein.profile.data.retrofit.ProfileApiEndPoint
import com.scenein.settings.data.retrofit.SettingsApiEndPoint
import com.scenein.tickets.data.retrofit.TicketApiEndPoint
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor // Import the logging interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.scenein.in/api/v1/"

    @Volatile
    private var retrofit: Retrofit? = null

    private fun buildRetrofit(context: Context): Retrofit {
        // 1. Create the logging interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Log request and response bodies
        }

        // 2. Create an OkHttpClient that uses BOTH your AuthInterceptor and the new LoggingInterceptor.
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context.applicationContext))
            .addInterceptor(loggingInterceptor) // Add the logger here
            .build()

        // 3. Build the Retrofit instance using this custom client.
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getInstance(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context).also { retrofit = it }
        }
    }

    private lateinit var retrofitInstance: Retrofit

    fun initialize(context: Context) {
        if (!::retrofitInstance.isInitialized) {
            retrofitInstance = getInstance(context)
        }
    }

    // All your lazy properties will now work with the new logging client
    val credentialApiEndPoint: CredentialApiEndPoint by lazy { retrofitInstance.create(CredentialApiEndPoint::class.java) }
    val createEventApiEndPoint: CreateEventApiEndPoint by lazy { retrofitInstance.create(CreateEventApiEndPoint::class.java) }
    val profileApiEndPoint: ProfileApiEndPoint by lazy { retrofitInstance.create(ProfileApiEndPoint::class.java) }
    val discoverApiEndPoint: DiscoverApiEndPoint by lazy { retrofitInstance.create(DiscoverApiEndPoint::class.java) }
    val chatApiEndPoint: ChatApiEndPoint by lazy { retrofitInstance.create(ChatApiEndPoint::class.java) }
    val ticketApiEndPoint: TicketApiEndPoint by lazy { retrofitInstance.create(TicketApiEndPoint::class.java) }
    val notificationApiEndPoint: NotificationApiEndPoint by lazy { retrofitInstance.create(NotificationApiEndPoint::class.java) }
    val settingsApiEndPoint: SettingsApiEndPoint by lazy { retrofitInstance.create(SettingsApiEndPoint::class.java) }
}
