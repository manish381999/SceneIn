package com.tie.vibein.discover.data.models

data class GetUsersResponse(
    val status: String,
    val message: String,
    val page: Int,
    val limit: Int,
    val total_users: Int,
    val users: List<User>
) {
    data class User(
        val user_id: Int,
        val mobile_number: String,
        val name: String,
        val user_name: String,
        val email_id: String,
        val profile_pic: String,
        val about_you: String,
        val interest: List<String>,
        val country_code: String,
        val country_short_name: String,
        val is_verified: Int,
        val status: Int,
        val deleted: Int,
        val created_at: String,
        val total_events_hosting: Int,
        val total_events_attending: Int
    )
}
