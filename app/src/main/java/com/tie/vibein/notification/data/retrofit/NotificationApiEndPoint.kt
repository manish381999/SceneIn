package com.tie.vibein.notification.data.retrofit

import com.tie.vibein.notifications.data.models.ActivityFeedResponse
import com.tie.vibein.tickets.data.models.GenericApiResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface NotificationApiEndPoint {

    @FormUrlEncoded
    @POST("api_v1/get_notifications.php")
    suspend fun getNotifications(@Field("user_id") userId: String): Response<ActivityFeedResponse>

    // Marks all notifications as read for a user
    @FormUrlEncoded
    @POST("api_v1/notifications_mark_read.php")
    suspend fun markNotificationsAsRead(@Field("user_id") userId: String): Response<GenericApiResponse> // Reuse your generic response
}