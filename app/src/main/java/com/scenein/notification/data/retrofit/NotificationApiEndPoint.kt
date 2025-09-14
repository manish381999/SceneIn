package com.scenein.notification.data.retrofit

import com.scenein.notification.data.models.ActivityFeedResponse
import com.scenein.tickets.data.models.GenericApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface NotificationApiEndPoint {


    @GET("notifications/get_notifications")
    suspend fun getNotifications(): Response<ActivityFeedResponse>

    // Marks all notifications as read for a user

    @POST("notifications/mark_notifications_read")
    suspend fun markNotificationsAsRead(): Response<GenericApiResponse> // Reuse your generic response
}