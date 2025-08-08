package com.tie.vibein.credentials.data.models

import com.google.gson.annotations.SerializedName

data class UsernameCheckResponse(
    @SerializedName("status") val status: String,
    @SerializedName("available") val available: Boolean,
    @SerializedName("message") val message: String
)
