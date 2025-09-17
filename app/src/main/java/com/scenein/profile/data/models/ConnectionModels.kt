package com.scenein.profile.data.models

import com.google.gson.annotations.SerializedName


data class ConnectionStatusResponse(
    val status: String,
    @SerializedName("connectionStatus")
    val connectionStatus: String,
    @SerializedName("requestSentBy")
    val requestSentBy: String?,
    // **NEW**: Added connectionId, which can be null if no connection exists
    @SerializedName("connectionId")
    val connectionId: String?
)

// This model is reused for send, remove, and respond actions
data class ConnectionActionResponse(
    val status: String,
    val message: String
)