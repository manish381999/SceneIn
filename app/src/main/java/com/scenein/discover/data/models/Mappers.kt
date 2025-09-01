package com.scenein.discover.data.models

/**
 * This extension function converts a detailed EventDetail object into the
 * EventSummary object used by the Discover feed's RecyclerView.
 */
fun EventDetail.toEventSummary(): EventSummary {
    return EventSummary(
        id = this.id,
        hostId = this.user_id,
        eventName = this.event_name ?: "",
        eventDescription = this.event_description,
        eventDate = this.event_date ?: "",
        startTime = this.start_time ?: "",
        venueLocation = this.venueLocation ?: "",
        city = this.city ?: "",
        coverImage = this.cover_image,
        maximumParticipants = this.maximum_participants,
        latitude = this.latitude,
        longitude = this.longitude,
        categoryName = this.category_name ?: "",
        hostDetails = this.host_details,
        participantsPreview = this.participants_preview,
        joinedParticipants = this.joined_participants,
        hasJoined = this.has_joined,
        isFull = this.is_full,

        // --- ADD THIS MISSING LINE ---
        hasBookmarked = this.has_bookmarked
    )
}