package com.scenein.discover.data.models

import com.google.gson.annotations.SerializedName

/**
 * This file contains data classes that are shared across multiple API responses,
 * such as EventSummary and EventDetail. Defining them here in one place prevents
 * "Redeclaration" errors and keeps the code clean.
 */

// Represents the details of an event host.
data class HostDetails(
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("profile_pic") val profilePic: String?
)

// Represents a single user in the participant preview list.
data class ParticipantPreview(
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("profile_pic") val profilePic: String?
)
