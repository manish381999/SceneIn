package com.scenein.discover.data.models

import com.google.gson.annotations.SerializedName

// Main response for get_suggested_connections.php
data class ConnectionsApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("connections") val connections: List<SuggestedConnection>
)

data class SuggestedConnection(
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("profile_pic") val profilePic: String?,
    @SerializedName("connection_status") val connectionStatus: String?,
    @SerializedName("mutual_interests_count") val mutualInterestsCount: Int,
    @SerializedName("request_sent_by") val requestSentBy: String?,
    @SerializedName("connection_id") val connectionId: String?,
)