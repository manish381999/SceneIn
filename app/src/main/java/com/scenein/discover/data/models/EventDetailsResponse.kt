package com.scenein.discover.data.models

import com.google.gson.annotations.SerializedName

// Main response object - No changes needed here
data class EventDetailResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("event") val event: EventDetail
)

// ✅ THIS CLASS HAS BEEN COMPLETELY UPDATED
// Full Event object, now matching the camelCase JSON response
data class EventDetail(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("eventName") val eventName: String?,
    @SerializedName("categoryId") val categoryId: String?,
    @SerializedName("eventDescription") val eventDescription: String?,
    @SerializedName("eventDate") val eventDate: String?,
    @SerializedName("startTime") val startTime: String?,
    @SerializedName("endTime") val endTime: String?,
    @SerializedName("eventDeliveryMode") val eventDeliveryMode: String?,
    @SerializedName("venueLocation") val venueLocation: String?,
    @SerializedName("meetingLink") val meetingLink: String?,
    @SerializedName("maximumParticipants") val maximumParticipants: String,
    @SerializedName("selectedAgeId") val selectedAgeId: String?,
    @SerializedName("selectedEventType") val selectedEventType: String,
    @SerializedName("ticketPrice") val ticketPrice: String?,
    @SerializedName("coverImage") val coverImage: String?,
    @SerializedName("createdDate") val createdDate: String?,
    @SerializedName("status") val status: String,
    @SerializedName("pincode") val pincode: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("fullAddress") val fullAddress: String?,
    @SerializedName("latitude") val latitude: String?,
    @SerializedName("longitude") val longitude: String?,
    @SerializedName("categoryName") val categoryName: String?,
    @SerializedName("ageRestriction") val ageRestriction: String?,
    @SerializedName("joinedParticipants") val joinedParticipants: Int,
    @SerializedName("hasJoined") val hasJoined: Boolean,
    @SerializedName("isFull") val isFull: Boolean,
    @SerializedName("hasBookmarked") val hasBookmarked: Boolean,

    // Nested objects
    @SerializedName("hostDetails") val hostDetails: HostDetails?,
    @SerializedName("participantsPreview") val participantsPreview: List<ParticipantPreview>
)