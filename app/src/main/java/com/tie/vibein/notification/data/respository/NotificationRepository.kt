package com.tie.vibein.notification.data.respository

import com.tie.vibein.credentials.data.retrofit.RetrofitClient

class NotificationRepository {
    // Assuming your Retrofit instance is accessible via RetrofitClient.apiEndPoint
    private val api = RetrofitClient.notificationApiEndPoint

    suspend fun getNotifications(userId: String) = api.getNotifications(userId)

    suspend fun markNotificationsAsRead(userId: String) = api.markNotificationsAsRead(userId)
}