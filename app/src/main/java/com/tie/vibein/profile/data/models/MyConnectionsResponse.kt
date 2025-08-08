package com.tie.vibein.profile.data.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MyConnectionsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("connections") val connections: List<ConnectionProfile>
)

// Represents a single user in your connections list
data class ConnectionProfile(
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("profile_pic") val profilePic: String?,
    @SerializedName("connection_id") val connectionId: String
) : Serializable // Serializable to allow passing via Intents if needed