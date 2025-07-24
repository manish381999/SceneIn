package com.tie.vibein.tickets.data.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Generic API response for simple success/error messages
data class GenericApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

// Response from /payouts_verify.php
data class PayoutVerificationResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("verifiedName") val verifiedName: String
)

data class MyActivityResponse(
    @SerializedName("status") val status: String,
    @SerializedName("listed_tickets") val listedTickets: List<Ticket>,
    @SerializedName("purchased_tickets") val purchasedTickets: List<Ticket>
)

// Represents a ticket in a list view (e.g., for browsing)


data class Ticket(
    @SerializedName("id") val id: Int,
    @SerializedName("seller_id") val sellerId: String,
    @SerializedName("event_name") val eventName: String,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("event_time") val eventTime: String,
    @SerializedName("event_venue") val eventVenue: String,
    @SerializedName("event_city") val event_city: String,
    @SerializedName("selling_price") val sellingPrice: String,
    @SerializedName("seller_name") val sellerName: String?,
    @SerializedName("original_price") val originalPrice: String,
    @SerializedName("seller_profile_pic") val sellerProfilePic: String?,
    @SerializedName("category_name") val category_name: String?,
    @SerializedName("status") val listingStatus: String, // 'live', 'sold', 'expired'
    @SerializedName("number_of_tickets") val numberOfTickets: Int,

    // Fields that only exist for PURCHASED tickets
    @SerializedName("transaction_id") val transactionId: String? = null,
    @SerializedName("transaction_status") val transactionStatus: String? = null, // 'escrow', 'completed_by_user', etc.
    @SerializedName("completion_type") val completionType: String? = null,
    @SerializedName("reveal_time") val revealTime: String? = null,
    @SerializedName("secure_file_path") val secureFilePath: String? = null // For showing the real ticket
) : Serializable

// Wrapper for the browse tickets API response
data class BrowseTicketsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("tickets") val tickets: List<Ticket>
)

// Data class for creating a payment order via Razorpay
data class CreateOrderResponse(
    @SerializedName("status") val status: String,
    @SerializedName("order_id") val orderId: String,
    @SerializedName("amount") val amountInPaise: Int,
    @SerializedName("key_id") val keyId: String,
    @SerializedName("message") val message: String? = null
)

// This is the definitive data class for the AI-parsed ticket information.
data class AnalyzedTicketData(
    @SerializedName("eventName")
    val eventName: String?,

    @SerializedName("eventVenue")
    val eventVenue: String?,

    @SerializedName("eventCity")
    val eventCity: String?,

    @SerializedName("eventDate")
    val eventDate: String?,

    @SerializedName("eventTime")
    val eventTime: String?,

    @SerializedName("originalPrice")
    val originalPrice: String, // Keep as String to handle "0.00"

    @SerializedName("requiresSeatNumber")
    val requiresSeatNumber: Boolean = false,

    @SerializedName("seatNumber")
    val seatNumber: String?,

    @SerializedName("ticketType")
    val ticketType: String?,

    @SerializedName("numberOfTickets")
    val numberOfTickets: Int?
)

// Wrapper for the text parsing API response
data class ParseTextResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: AnalyzedTicketData,
    @SerializedName("message") val message: String? = null

)

