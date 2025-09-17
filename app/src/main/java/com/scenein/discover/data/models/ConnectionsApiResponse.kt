package com.scenein.discover.data.models

import com.google.gson.annotations.SerializedName

// Main response for get_suggested_connections.php
data class ConnectionsApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("connections") val connections: List<SuggestedConnection>
)

data class SuggestedConnection(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("userName") val userName: String,
    @SerializedName("profilePic") val profilePic: String?,
    @SerializedName("connectionStatus") val connectionStatus: String?,
    @SerializedName("mutualInterestsCount") val mutualInterestsCount: Int,
    @SerializedName("requestSentBy") val requestSentBy: String?,
    @SerializedName("connectionId") val connectionId: String?,
)