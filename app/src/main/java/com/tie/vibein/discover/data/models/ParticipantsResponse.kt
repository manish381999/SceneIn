package com.tie.vibein.discover.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

// In your models package, e.g., discover/data/models
@Parcelize
data class Participant(
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("profile_pic") val profilePic: String?,
    @SerializedName("connection_status") var connectionStatus: String
) : Parcelable

data class ParticipantsResponse(
    val status: String,
    val participants: List<Participant>
)