package com.tie.vibein.credentials.data.models

data class VerifyOtpResponse(
    val status: String,
    val message: String,
    val user: UserData? = null
)

data class UserData(
    val user_id: String, // Changed to String to match the response format
    val mobile_number: String,
    val name: String?,
    val user_name: String?,
    val email_id: String?,
    val profile_pic: String?, // Full URL included
    val about_you: String?,
    val country_code: String?,
    val country_short_name: String?,
    val is_verified: Boolean, // Changed to String to match "true" in quotes
    val status: String, // Already a string in response
    val deleted: String, // Already a string in response
    val created_at: String,
    val fcm_token: String?, // New field
    val payout_method_type: String?, // Nullable as it's null in response
    val payout_info_display: String?, // Nullable as it's null in response
    val payout_method_verified: Boolean,
    val interest_names: List<String> // List of strings
)
