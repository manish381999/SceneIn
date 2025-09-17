package com.scenein.settings.data.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Model for the LIST screen
data class TransactionHistoryItem(
    @SerializedName("transactionId")
    val transactionId: String,

    @SerializedName("transactionStatus")
    val transactionStatus: String, // e.g., "escrow", "payout_processed"

    @SerializedName("finalAmount")
    val finalAmount: String, // The total amount the buyer paid (e.g., "1050.00")

    @SerializedName("sellerPayoutAmount")
    val sellerPayoutAmount: String, // The amount the seller received (e.g., "900.00")

    @SerializedName("createdAt")
    val createdAt: String, // The timestamp when the transaction was created

    @SerializedName("eventName")
    val eventName: String,

    @SerializedName("userRole")
    val userRole: String,

    @SerializedName("otherUserName")
    val otherUserName: String

) : Serializable
// Wrapper for the LIST API response
data class TransactionHistoryResponse(
    @SerializedName("status") val status: String,
    @SerializedName("transactions") val transactions: List<TransactionHistoryItem>
)

// --- NEW, DETAILED MODEL for the DETAIL screen ---
data class TransactionDetail(
    @SerializedName("transactionId") val transactionId: String,
    @SerializedName("finalAmount") val finalAmount: String,
    @SerializedName("sellerPayoutAmount") val sellerPayoutAmount: String,
    @SerializedName("platformFee") val platformFee: String,
    @SerializedName("transactionStatus") val transactionStatus: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("eventName") val eventName: String,
    @SerializedName("eventVenue") val eventVenue: String,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("eventTime") val eventTime: String,
    @SerializedName("userRole") val userRole: String, // "BOUGHT" or "SOLD"
    @SerializedName("otherUserName") val otherUserName: String,
    @SerializedName("otherUserProfilePic") val otherUserProfilePic: String?,
    @SerializedName("receiptId") val receiptId: String
) : Serializable

// Wrapper for the DETAIL API response
data class TransactionDetailResponse(
    @SerializedName("status") val status: String,
    @SerializedName("transaction") val transaction: TransactionDetail
)