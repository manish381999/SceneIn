package com.scenein.tickets.data.models

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
    @SerializedName("listedTickets") val listedTickets: List<Ticket>,
    @SerializedName("purchasedTickets") val purchasedTickets: List<Ticket>
)

// Represents a ticket in a list view (e.g., for browsing)


data class Ticket(
    @SerializedName("id") val id: String,
    @SerializedName("sellerId") val sellerId: String,
    @SerializedName("eventName") val eventName: String,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("eventTime") val eventTime: String,
    @SerializedName("eventVenue") val eventVenue: String,
    @SerializedName("eventCity") val eventCity: String,
    @SerializedName("sellingPrice") val sellingPrice: String,
    @SerializedName("sellerName") val sellerName: String? = null,
    @SerializedName("originalPrice") val originalPrice: String,
    @SerializedName("sellerProfilePic") val sellerProfilePic: String? = null,
    @SerializedName("category_name") val category_name: String? = null,
    @SerializedName("status") val listingStatus: String, // 'live', 'sold', 'expired'
    @SerializedName("numberOfTickets") val numberOfTickets: Int,

    // Purchased tickets only
    @SerializedName("transactionId") val transactionId: String? = null,
    @SerializedName("transactionStatus") val transactionStatus: String? = null,
    @SerializedName("completionType") val completionType: String? = null,
    @SerializedName("revealTime") val revealTime: String? = null,
    @SerializedName("secureFilePath") val secureFilePath: String? = null
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

