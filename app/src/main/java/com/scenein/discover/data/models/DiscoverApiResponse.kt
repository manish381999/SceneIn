package com.scenein.discover.data.models

import com.google.gson.annotations.SerializedName

// Main response for get_events_by_city.php
data class DiscoverApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String? = null,
    @SerializedName("events") val events: List<EventSummary>
)

data class EventSummary(
    @SerializedName("id") val id: String,
    @SerializedName("host_id") val hostId: String,
    @SerializedName("eventName") val eventName: String,
    @SerializedName("eventDescription") val eventDescription: String? = null,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("venueLocation") val venueLocation: String,
    @SerializedName("city") val city: String,
    @SerializedName("coverImage") val coverImage: String? = null,
    @SerializedName("maximumParticipants") val maximumParticipants: String,
    @SerializedName("latitude") val latitude: String? = null,
    @SerializedName("longitude") val longitude: String? = null,
    @SerializedName("categoryName") val categoryName: String,
    @SerializedName("hostDetails") val hostDetails: HostDetails?,
    @SerializedName("participantsPreview")
    val participantsPreview: List<ParticipantPreview> = emptyList(),
    @SerializedName("joinedParticipants") val joinedParticipants: Int,
    @SerializedName("hasJoined") val hasJoined: Boolean,
    @SerializedName("isFull") val isFull: Boolean,
    @SerializedName("hasBookmarked") var hasBookmarked: Boolean
)
