package com.scenein.notification.data.retrofit

import com.scenein.notification.data.models.ActivityFeedResponse
import com.scenein.tickets.data.models.GenericApiResponse
import retrofit2.Response
import retrofit2.http.POST

interface NotificationApiEndPoint {


    @POST("api_v1/get_notifications.php")
    suspend fun getNotifications(): Response<ActivityFeedResponse>

    // Marks all notifications as read for a user

    @POST("api_v1/notifications_mark_read.php")
    suspend fun markNotificationsAsRead(): Response<GenericApiResponse> // Reuse your generic response
}