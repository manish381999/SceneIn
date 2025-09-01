package com.scenein

import android.app.Application
import com.scenein.credentials.data.retrofit.RetrofitClient
import com.scenein.utils.ThemeManager

class SceneInApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Apply the user's saved theme preference as soon as the app starts.
        ThemeManager.applyTheme(this)

        // --- THIS IS THE CRITICAL INITIALIZATION STEP ---
        // We initialize the RetrofitClient with the application context.
        // This sets up the AuthInterceptor for the entire app lifecycle.
        RetrofitClient.initialize(this)
    }
}