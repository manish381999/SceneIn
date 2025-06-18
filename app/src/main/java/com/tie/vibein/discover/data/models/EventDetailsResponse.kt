package com.tie.vibein.discover.data.models
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class EventDetailsResponse(
    val status: String,
    val message: String,
    val event: EventDetail
) : Parcelable

@Parcelize
data class EventDetail(
    val id: String,
    val user_id: String,
    val event_name: String,
    val category_id: String,
    val event_description: String,
    val event_date: String,
    val start_time: String,
    val end_time: String,
    val eventDeliveryMode: String,
    val venueLocation: String?,
    val meetingLink: String?,
    val maximum_participants: String,
    val selectedEventType: String,
    val ticketPrice: String,
    val cover_image: String?,
    val created_date: String,
    val status: String,
    val pincode: String?,
    val city: String?,
    val full_address: String?,
    val category_name: String?,
    val age_restriction: String?,
    val participants_user_id: String?,
    val joined_participants: Int?,
    val role: String?,
    val has_joined: Boolean,
    val is_full: Boolean
) : Parcelable