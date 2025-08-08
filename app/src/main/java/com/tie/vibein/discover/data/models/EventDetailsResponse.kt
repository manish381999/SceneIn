package com.tie.vibein.discover.data.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// This represents the main response from your get_event_details_by_id.php
data class EventDetailResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("event") val event: EventDetail
)

// This is the complete object for a single event
data class EventDetail(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val user_id: String,
    @SerializedName("event_name") val event_name: String?,
    @SerializedName("event_description") val event_description: String?,
    @SerializedName("event_date") val event_date: String?,
    @SerializedName("start_time") val start_time: String?,
    @SerializedName("end_time") val end_time: String?,
    @SerializedName("eventDeliveryMode") val eventDeliveryMode: String?,
    @SerializedName("venueLocation") val venueLocation: String?,
    @SerializedName("maximum_participants") val maximum_participants: String,
    @SerializedName("ticketPrice") val ticketPrice: String?,
    @SerializedName("cover_image") val cover_image: String?,
    @SerializedName("category_name") val category_name: String?,
    @SerializedName("age_restriction") val age_restriction: String?,
    @SerializedName("joined_participants") val joined_participants: Int,
    @SerializedName("has_joined") val has_joined: Boolean,
    @SerializedName("is_full") val is_full: Boolean,
    // --- The new, nested objects ---
    @SerializedName("host_details") val host_details: HostDetails?,
    @SerializedName("participants_preview") val participants_preview: List<ParticipantPreview>
)

// A simple data class for the host's details
data class HostDetails(
    @SerializedName("user_id") val user_id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("user_name") val user_name: String?,
    @SerializedName("profile_pic") val profile_pic: String?
)

// A simple data class for the participants in the preview list
data class ParticipantPreview(
    @SerializedName("user_id") val user_id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("user_name") val user_name: String?,
    @SerializedName("profile_pic") val profile_pic: String?
)