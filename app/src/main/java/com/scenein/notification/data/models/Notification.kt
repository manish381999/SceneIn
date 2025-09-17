package com.scenein.notification.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// The main response object from the get_notifications.php API.
// This class itself is not passed between screens, so it doesn't need to be Parcelable.
data class ActivityFeedResponse(
    @SerializedName("status") val status: String,
    @SerializedName("connectionRequests") val connectionRequests: List<ConnectionRequest>,
    @SerializedName("activityFeed") val activityFeed: List<Notification>
)

/**
 * Represents a pending connection request item.
 * @Parcelize allows this object to be passed efficiently in an Intent.
 */
@Parcelize
data class ConnectionRequest(
    @SerializedName("connectionId") val connectionId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("userName") val userName: String?,
    @SerializedName("profilePic") val profilePic: String?,
    @SerializedName("createdAt") val createdAt: String
) : Parcelable

/**
 * Represents a single item in the main activity feed (e.g., event join, ticket sold).
 * @Parcelize allows this object to be passed to detail screens.
 */
@Parcelize
data class Notification(
    @SerializedName("notificationId") val notificationId: Int,
    @SerializedName("userId") val userId: String?,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("notificationType") val notificationType: String,
    @SerializedName("relatedId") val relatedId: String?,
    @SerializedName("actorId") val actorId: String?,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("createdAt") val createdAt: String?,

    // --- All fields related to the "actor" (the person who caused the notification) ---
    @SerializedName("relatedUserId") val relatedUserId: String?,
    @SerializedName("relatedUserName") val relatedUserName: String?,

    // --- THIS IS THE NEWLY ADDED FIELD ---
    @SerializedName("relatedUserUsername") val relatedUserUsername: String?,

    @SerializedName("relatedUserProfilePic") val relatedUserProfilePic: String?,

    // --- Fields related to the event/ticket context ---
    @SerializedName("eventName") val eventName: String?,
    @SerializedName("eventImageUrl") val eventImageUrl: String?
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