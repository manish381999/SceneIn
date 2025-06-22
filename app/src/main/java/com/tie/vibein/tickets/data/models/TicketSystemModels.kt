package com.tie.vibein.tickets.data.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// A simple, generic response for success/error messages.
data class GenericApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

// --- Seller Flow Models ---

// Used for the response from /payouts_verify.php
data class PayoutVerificationResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("verifiedName") val verifiedName: String
)

// Used for the response from /tickets_create.php (It uses the GenericApiResponse)


// --- Buyer Flow Models ---

// Represents a single ticket in a list view.
// Used by tickets_browse.php and my_listings.php
data class Ticket(
    @SerializedName("id") val id: Int,
    @SerializedName("seller_id") val sellerId: String,
    @SerializedName("event_name") val eventName: String,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("event_venue") val eventVenue: String,
    @SerializedName("selling_price") val sellingPrice: String,
    @SerializedName("status") val status: String,
    // These fields are optional as they only appear in the browse endpoint
    @SerializedName("seller_name") val sellerName: String? = null,
    @SerializedName("seller_profile_pic") val sellerProfilePic: String? = null
) : Serializable // Serializable to allow passing between Activities/Fragments

// Wrapper for the response from /tickets_browse.php
data class BrowseTicketsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("tickets") val tickets: List<Ticket>
)

// Wrapper for the response from a potential /my_listings.php endpoint
data class MyListingsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("tickets") val tickets: List<Ticket>
)

// Represents the data needed to start a Razorpay transaction.
// Used for the response from /orders_create.php
data class CreateOrderResponse(
    @SerializedName("status") val status: String,
    @SerializedName("order_id") val orderId: String,
    @SerializedName("amount") val amountInPaise: Int,
    @SerializedName("key_id") val keyId: String,
    // Add message for potential errors from the server
    @SerializedName("message") val message: String? = null
)

// --- FCM & Notification Models (if you handle them via data classes) ---

// Represents the data payload for a "Ticket Sold" notification
data class TicketSoldNotification(
    @SerializedName("notification_type") val notificationType: String, // "ticket_sold"
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("ticket_id") val ticketId: String
)

// Represents the data payload for a "Payout Processed" notification
data class PayoutProcessedNotification(
    @SerializedName("notification_type") val notificationType: String, // "payout_processed"
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("transaction_id") val transactionId: String
)