package com.tie.vibein.profile.data.models

import com.google.gson.annotations.SerializedName

// For check_connection_status.php
data class ConnectionStatusResponse(
    val status: String,
    @SerializedName("connection_status")
    val connectionStatus: String,
    @SerializedName("request_sent_by")
    val requestSentBy: String?,
    // **NEW**: Added connectionId, which can be null if no connection exists
    @SerializedName("connection_id")
    val connectionId: Int?
)

// This model is reused for send, remove, and respond actions
data class ConnectionActionResponse(
    val status: String,
    val message: String
)