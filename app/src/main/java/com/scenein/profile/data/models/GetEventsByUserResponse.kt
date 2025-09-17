package com.scenein.profile.data.models

data class GetEventsByUserResponse(
    val status: String,
    val message: String,
    val events: List<Event>
)

data class Event(
    val id: String,
    val user_id: String,
    val event_name: String,
    val event_description: String,
    val event_date: String,
    val start_time: String,
    val end_time: String,
    val eventDeliveryMode: String,
    val venueLocation: String,
    val meetingLink: String,
    val maximum_participants: String,
    val selectedEventType: String,
    val ticketPrice: String,
    val cover_image: String?, // Nullable in case it's null
    val created_date: String,
    val status: String,
    val pincode: String,
    val city: String,
    val full_address: String,
    val category_name: String,
    val age_restriction: String?, // Nullable if not present
    val role: String,
    val participants_user_id: List<String>,
    val joined_participants: String
)

