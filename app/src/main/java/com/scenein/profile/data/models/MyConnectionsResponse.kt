package com.scenein.profile.data.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MyConnectionsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("connections") val connections: List<ConnectionProfile>
)

// Represents a single user in your connections list
data class ConnectionProfile(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("userName") val userName: String?,
    @SerializedName("profilePic") val profilePic: String?,
    @SerializedName("connectionId") val connectionId: String
) : Serializable // Serializable to allow passing via Intents if needed