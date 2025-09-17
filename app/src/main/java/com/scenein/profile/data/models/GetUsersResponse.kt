package com.scenein.profile.data.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Main API response for user profile
data class GetUserResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: PublicProfileData
) : Serializable

// Data class representing public profile fields
data class PublicProfileData(
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("profile_pic") val profilePic: String?,
    @SerializedName("about_you") val aboutYou: String?,
    @SerializedName("total_events_hosting") val totalEventsHosting: String,
    @SerializedName("total_events_attending") val totalEventsAttending: String,
    @SerializedName("total_tickets_sold") val totalTicketsSold: String,
    @SerializedName("total_tickets_bought") val totalTicketsBought: String,
    @SerializedName("total_connections") val totalConnections: String,
    @SerializedName("interest_names") val interestNames: List<String>
) : Serializable
