package com.scenein.discover.data.models

import com.scenein.createEvent.data.models.Category

/**
 * A sealed class to represent the different types of items in the Discover feed.
 */
sealed class FeedItem {
    // <<< The Header data class that was here has been DELETED >>>

    /**
     * Represents a single event card.
     */
    data class Event(val eventSummary: EventSummary) : FeedItem()

    /**
     * Represents the horizontal list of suggested connections.
     * @param title The title for the section (e.g., "People You May Know").
     * @param connections The list of suggested users.
     */
    data class Connections(
        val title: String,
        val connections: List<SuggestedConnection>
    ) : FeedItem()
}