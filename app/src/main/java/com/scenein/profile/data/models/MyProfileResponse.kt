package com.scenein.profile.data.models

import com.google.gson.annotations.SerializedName

// --- NEW: For the get_my_profile.php response ---
data class MyProfileResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: MyProfileData
)

data class MyProfileData(
    // Includes all public AND private fields
    @SerializedName("user_id") val userId: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("mobile_number") val mobileNumber: String?,
    @SerializedName("email_id") val emailId: String?,
    @SerializedName("profile_pic") val profilePic: String?,
    @SerializedName("about_you") val aboutYou: String?,
    @SerializedName("is_verified") val isVerified: Boolean,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("payout_method_type") val payoutMethodType: String?,
    @SerializedName("payout_method_verified") val payoutMethodVerified: Boolean,
    @SerializedName("payout_info_display") val payoutInfoDisplay: String?,
    @SerializedName("total_events_hosting") val totalEventsHosting: Int,
    @SerializedName("total_events_attending") val totalEventsAttending: Int,
    @SerializedName("total_tickets_sold") val totalTicketsSold: Int,
    @SerializedName("total_tickets_bought") val totalTicketsBought: Int,
    @SerializedName("total_connections") val totalConnections: Int,
    @SerializedName("interest_names") val interestNames: List<String>
)