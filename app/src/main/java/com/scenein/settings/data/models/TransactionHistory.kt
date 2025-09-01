package com.scenein.settings.data.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Model for the LIST screen
data class TransactionHistoryItem(
    @SerializedName("id")
    val transactionId: Int,

    @SerializedName("transaction_status")
    val transactionStatus: String, // e.g., "escrow", "payout_processed"

    @SerializedName("final_amount")
    val finalAmount: String, // The total amount the buyer paid (e.g., "1050.00")

    @SerializedName("seller_payout_amount")
    val sellerPayoutAmount: String, // The amount the seller received (e.g., "900.00")

    @SerializedName("created_at")
    val createdAt: String, // The timestamp when the transaction was created

    @SerializedName("event_name")
    val eventName: String,

    @SerializedName("user_role")
    val userRole: String,

    @SerializedName("other_user_name")
    val otherUserName: String

) : Serializable
// Wrapper for the LIST API response
data class TransactionHistoryResponse(
    @SerializedName("status") val status: String,
    @SerializedName("transactions") val transactions: List<TransactionHistoryItem>
)

// --- NEW, DETAILED MODEL for the DETAIL screen ---
data class TransactionDetail(
    @SerializedName("transaction_id") val transactionId: Int,
    @SerializedName("final_amount") val finalAmount: String,
    @SerializedName("seller_payout_amount") val sellerPayoutAmount: String,
    @SerializedName("platform_fee") val platformFee: String,
    @SerializedName("transaction_status") val transactionStatus: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("event_name") val eventName: String,
    @SerializedName("event_venue") val eventVenue: String,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("event_time") val eventTime: String,
    @SerializedName("user_role") val userRole: String, // "BOUGHT" or "SOLD"
    @SerializedName("other_user_name") val otherUserName: String,
    @SerializedName("other_user_profile_pic") val otherUserProfilePic: String?,
    @SerializedName("receipt_id") val receiptId: String
) : Serializable

// Wrapper for the DETAIL API response
data class TransactionDetailResponse(
    @SerializedName("status") val status: String,
    @SerializedName("transaction") val transaction: TransactionDetail
)