package com.scenein.settings.data.models

import com.google.gson.annotations.SerializedName

// Reusable response for actions like logout, delete account, etc.
data class SettingsActionResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("action") val action: String? = null
)