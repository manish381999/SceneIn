package com.scenein.discover.data.models

import com.google.gson.annotations.SerializedName

// Main response for get_events_by_city.php
data class DiscoverApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("events") val events: List<EventSummary>
)

data class EventSummary(
    @SerializedName("id") val id: String,
    @SerializedName("host_id") val hostId: String,
    @SerializedName("event_name") val eventName: String,
    @SerializedName("event_description") val eventDescription: String?,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("venueLocation") val venueLocation: String,
    @SerializedName("city") val city: String,
    @SerializedName("cover_image") val coverImage: String?,
    @SerializedName("maximum_participants") val maximumParticipants: String,
    @SerializedName("latitude") val latitude: String?,
    @SerializedName("longitude") val longitude: String?,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("host_details") val hostDetails: HostDetails?,
    @SerializedName("participants_preview") val participantsPreview: List<ParticipantPreview>,
    @SerializedName("joined_participants") val joinedParticipants: Int,
    @SerializedName("has_joined") val hasJoined: Boolean,
    @SerializedName("is_full") val isFull: Boolean,
    @SerializedName("has_bookmarked") var hasBookmarked: Boolean

)
