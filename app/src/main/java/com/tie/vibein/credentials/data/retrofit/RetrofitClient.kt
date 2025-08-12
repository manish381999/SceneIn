package com.tie.vibein.credentials.data.retrofit

import android.content.Context
import com.tie.vibein.chat.data.retrofit.ChatApiEndPoint
import com.tie.vibein.createEvent.data.retrofit.CreateEventApiEndPoint
import com.tie.vibein.discover.data.retrofit.DiscoverApiEndPoint
import com.tie.vibein.notification.data.retrofit.NotificationApiEndPoint
import com.tie.vibein.profile.data.retrofit.ProfileApiEndPoint
import com.tie.vibein.settings.data.retrofit.SettingsApiEndPoint
import com.tie.vibein.tickets.data.retrofit.TicketApiEndPoint
import com.tie.vibein.utils.network.AuthInterceptor // Import your interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// This is now the definitive, production-ready RetrofitClient Singleton.
object RetrofitClient {
    private const val BASE_URL = "https://dreamsquad.fun/"

    // The Retrofit instance is now nullable and will be initialized once.
    @Volatile
    private var retrofit: Retrofit? = null

    // This private function builds the Retrofit instance with the interceptor.
    private fun buildRetrofit(context: Context): Retrofit {
        // 1. Create an OkHttpClient that uses our AuthInterceptor.
        // This client will now automatically add the auth token to every call.
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context.applicationContext))
            .build()

        // 2. Build the Retrofit instance using this custom client.
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // <-- Using the new, smart OkHttpClient
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // This is the public function to get the Retrofit instance.
    // It uses a double-checked lock to ensure it's only initialized once.
    fun getInstance(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context).also { retrofit = it }
        }
    }

    // --- We no longer need the lazy initializers for every single endpoint ---
    // Instead, different parts of your app will create the service they need
    // after getting the singleton instance. For example, in a Repository:
    // val api = RetrofitClient.getInstance(context).create(ApiService::class.java)

    // However, to maintain your existing structure without major refactoring,
    // we need to adapt the lazy properties to depend on an initialized client.
    // This requires an initialization step in your Application class.

    private lateinit var retrofitInstance: Retrofit

    fun initialize(context: Context) {
        if (!::retrofitInstance.isInitialized) {
            retrofitInstance = getInstance(context)
        }
    }

    // Now, your lazy properties will work after initialization.
    val apiService: ApiService by lazy { retrofitInstance.create(ApiService::class.java) }
    val createEventApiEndPoint: CreateEventApiEndPoint by lazy { retrofitInstance.create(CreateEventApiEndPoint::class.java) }
    val profileApiEndPoint: ProfileApiEndPoint by lazy { retrofitInstance.create(ProfileApiEndPoint::class.java) }
    val discoverApiEndPoint: DiscoverApiEndPoint by lazy { retrofitInstance.create(DiscoverApiEndPoint::class.java) }
    val chatApiEndPoint: ChatApiEndPoint by lazy { retrofitInstance.create(ChatApiEndPoint::class.java) }
    val ticketApiEndPoint: TicketApiEndPoint by lazy { retrofitInstance.create(TicketApiEndPoint::class.java) }
    val notificationApiEndPoint: NotificationApiEndPoint by lazy { retrofitInstance.create(NotificationApiEndPoint::class.java) }
    val settingsApiEndPoint: SettingsApiEndPoint by lazy { retrofitInstance.create(SettingsApiEndPoint::class.java) }
}