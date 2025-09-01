package com.scenein.notification.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// The main response object from the get_notifications.php API.
// This class itself is not passed between screens, so it doesn't need to be Parcelable.
data class ActivityFeedResponse(
    @SerializedName("status") val status: String,
    @SerializedName("connection_requests") val connectionRequests: List<ConnectionRequest>,
    @SerializedName("activity_feed") val activityFeed: List<Notification>
)

/**
 * Represents a pending connection request item.
 * @Parcelize allows this object to be passed efficiently in an Intent.
 */
@Parcelize
data class ConnectionRequest(
    @SerializedName("connection_id") val connectionId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("profile_pic") val profilePic: String?,
    @SerializedName("created_at") val createdAt: String
) : Parcelable

/**
 * Represents a single item in the main activity feed (e.g., event join, ticket sold).
 * @Parcelize allows this object to be passed to detail screens.
 */
@Parcelize
data class Notification(
    @SerializedName("notification_id") val notification_id: Int,
    @SerializedName("user_id") val user_id: String?,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("notification_type") val notification_type: String,
    @SerializedName("related_id") val related_id: String?,
    @SerializedName("actor_id") val actorId: String?,
    @SerializedName("is_read") val is_read: Boolean,
    @SerializedName("created_at") val created_at: String?,

    // --- All fields related to the "actor" (the person who caused the notification) ---
    @SerializedName("related_user_id") val related_user_id: String?,
    @SerializedName("related_user_name") val related_user_name: String?,

    // --- THIS IS THE NEWLY ADDED FIELD ---
    @SerializedName("related_user_username") val related_user_username: String?,

    @SerializedName("related_user_profile_pic") val related_user_profile_pic: String?,

    // --- Fields related to the event/ticket context ---
    @SerializedName("event_name") val event_name: String?,
    @SerializedName("event_image_url") val eventImageUrl: String?
) : Parcelable

/**
 * A helper data class for the main NotificationsAdapter to represent the
 * "Connection Requests" header as a single item in the RecyclerView.
 * This does not need to be Parcelable.
 */
data class HeaderData(
    val count: Int,
    val requests: List<ConnectionRequest>
)