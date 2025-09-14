package com.scenein.discover.data.models

/**
 * This extension function converts a detailed EventDetail object into the
 * EventSummary object used by the Discover feed's RecyclerView.
 * This version is updated to use camelCase properties from the EventDetail class.
 */
fun EventDetail.toEventSummary(): EventSummary {
    return EventSummary(
        id = this.id,
        hostId = this.userId, // Updated
        eventName = this.eventName ?: "", // Updated
        eventDescription = this.eventDescription, // Updated
        eventDate = this.eventDate ?: "", // Updated
        startTime = this.startTime ?: "", // Updated
        venueLocation = this.venueLocation ?: "",
        city = this.city ?: "",
        coverImage = this.coverImage, // Updated
        maximumParticipants = this.maximumParticipants, // Updated
        latitude = this.latitude,
        longitude = this.longitude,
        categoryName = this.categoryName ?: "", // Updated
        hostDetails = this.hostDetails, // Updated
        participantsPreview = this.participantsPreview, // Updated
        joinedParticipants = this.joinedParticipants, // Updated
        hasJoined = this.hasJoined, // Updated
        isFull = this.isFull, // Updated
        hasBookmarked = this.hasBookmarked // Updated
    )
}