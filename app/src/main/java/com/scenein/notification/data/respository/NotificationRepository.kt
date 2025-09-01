package com.scenein.notification.data.respository

import com.scenein.credentials.data.retrofit.RetrofitClient

class NotificationRepository {
    // Assuming your Retrofit instance is accessible via RetrofitClient.apiEndPoint
    private val api = RetrofitClient.notificationApiEndPoint

    suspend fun getNotifications() = api.getNotifications()

    suspend fun markNotificationsAsRead() = api.markNotificationsAsRead()
}