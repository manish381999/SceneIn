package com.scenein.discover.data.models

import com.google.gson.annotations.SerializedName

// Main response object
data class EventDetailResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("event") val event: EventDetail
)

// Full Event object
data class EventDetail(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val user_id: String,
    @SerializedName("event_name") val event_name: String?,
    @SerializedName("category_id") val category_id: String?,
    @SerializedName("event_description") val event_description: String?,
    @SerializedName("event_date") val event_date: String?,
    @SerializedName("start_time") val start_time: String?,
    @SerializedName("end_time") val end_time: String?,
    @SerializedName("eventDeliveryMode") val eventDeliveryMode: String?,
    @SerializedName("venueLocation") val venueLocation: String?,
    @SerializedName("meetingLink") val meetingLink: String?,
    @SerializedName("maximum_participants") val maximum_participants: String,
    @SerializedName("selectedAgeId") val selectedAgeId: String?,
    @SerializedName("selectedEventType") val selectedEventType: String,
    @SerializedName("ticketPrice") val ticketPrice: String?,
    @SerializedName("cover_image") val cover_image: String?,
    @SerializedName("created_date") val created_date: String?,
    @SerializedName("status") val status: Int,
    @SerializedName("pincode") val pincode: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("full_address") val full_address: String?,
    @SerializedName("latitude") val latitude: String?,
    @SerializedName("longitude") val longitude: String?,
    @SerializedName("category_name") val category_name: String?,
    @SerializedName("age_restriction") val age_restriction: String?,
    @SerializedName("joined_participants") val joined_participants: Int,
    @SerializedName("has_joined") val has_joined: Boolean,
    @SerializedName("is_full") val is_full: Boolean,
    @SerializedName("has_bookmarked") val has_bookmarked: Boolean,
    // Nested objects
    @SerializedName("host_details") val host_details: HostDetails?,
    @SerializedName("participants_preview") val participants_preview: List<ParticipantPreview>
)

//// Host details
//data class HostDetails(
//    @SerializedName("user_id") val user_id: Int,
//    @SerializedName("name") val name: String?,
//    @SerializedName("user_name") val user_name: String?,
//    @SerializedName("profile_pic") val profile_pic: String?
//)
//
//// Participant preview
//data class ParticipantPreview(
//    @SerializedName("user_id") val user_id: Int,
//    @SerializedName("name") val name: String?,
//    @SerializedName("user_name") val user_name: String?,
//    @SerializedName("profile_pic") val profile_pic: String?
//)
