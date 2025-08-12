package com.tie.vibein.credentials.data.models

import com.google.gson.annotations.SerializedName

// The top-level response wrapper. It now includes the 'auth_token'.
data class VerifyOtpResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("auth_token") val authToken: String? = null, // The critical session token
    @SerializedName("user") val user: UserData? = null
)

// The definitive UserData class. The field names and data types now
// perfectly match the JSON response from the final backend script.
data class UserData(
    @SerializedName("user_id")
    val userId: String,

    @SerializedName("mobile_number")
    val mobileNumber: String,

    @SerializedName("name")
    val name: String?,

    @SerializedName("user_name")
    val userName: String?,

    @SerializedName("email_id")
    val emailId: String?,

    @SerializedName("profile_pic")
    val profilePic: String?,

    @SerializedName("about_you")
    val aboutYou: String?,

    @SerializedName("country_code")
    val countryCode: String?,

    @SerializedName("country_short_name")
    val countryShortName: String?,

    @SerializedName("is_verified")
    val isVerified: Boolean,

    @SerializedName("status")
    val status: String,

    @SerializedName("deleted")
    val deleted: String,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("fcm_token")
    val fcmToken: String?,

    @SerializedName("payout_method_type")
    val payoutMethodType: String?,

    // Note: payout_details_encrypted is correctly NOT sent to the client

    @SerializedName("payout_method_verified")
    val payoutMethodVerified: Boolean,

    @SerializedName("payout_info_display")
    val payoutInfoDisplay: String?,

    @SerializedName("interest_names")
    val interestNames: List<String>
)