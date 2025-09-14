package com.scenein.discover.data.models

import com.google.gson.annotations.SerializedName

// Represents the details of an event host.
data class HostDetails(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("userName") val userName: String?,
    @SerializedName("profilePic") val profilePic: String?
)

// Represents a single user in the participant preview list.
data class ParticipantPreview(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("userName") val userName: String?,
    @SerializedName("profilePic") val profilePic: String?
)
