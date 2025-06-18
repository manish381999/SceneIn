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
    val profile_pic: String?, // Full URL now included
    val about_you: String?,
    val country_code: String?,
    val country_short_name: String?,
    val is_verified: String, // Changed to String to match the response format
    val status: String, // Changed to String to match the response format
    val deleted: String, // Changed to String to match the response format
    val created_at: String,
    val interest_names: List<String> // New field to store interest names
)
